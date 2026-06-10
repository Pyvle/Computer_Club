import os
import random
from datetime import datetime, timedelta

from locust import HttpUser, LoadTestShape, between, task
from locust.exception import StopUser


def env_bool(name: str, default: bool = False) -> bool:
    value = os.getenv(name)
    if value is None:
        return default
    return value.lower() in {"1", "true", "yes", "on"}


LOCUST_CLUB_ID = os.getenv("LOCUST_CLUB_ID")
LOCUST_ADMIN_CLUB_ID = os.getenv("LOCUST_ADMIN_CLUB_ID")
ENABLE_CLIENT_AUTH = env_bool("LOCUST_ENABLE_CLIENT_AUTH", default=False)
USE_STEP_LOAD = env_bool("LOCUST_USE_STEP_LOAD", default=False)
CLIENT_PHONE = os.getenv("LOCUST_CLIENT_PHONE")
CLIENT_PHONE_PREFIX = os.getenv("LOCUST_CLIENT_PHONE_PREFIX", "+7999000")
ADMIN_PHONE = os.getenv("LOCUST_ADMIN_PHONE")
ADMIN_PASSWORD = os.getenv("LOCUST_ADMIN_PASSWORD")
ENABLE_WRITE_TASKS = env_bool("LOCUST_ENABLE_WRITE_TASKS", default=False)
TIME_OFFSET_HOURS = int(os.getenv("LOCUST_TIME_OFFSET_HOURS", "2"))
TIME_WINDOW_HOURS = int(os.getenv("LOCUST_TIME_WINDOW_HOURS", "2"))
STEP_SPAWN_RATE = float(os.getenv("LOCUST_STEP_SPAWN_RATE", "5"))
STEP_STAGES_RAW = os.getenv("LOCUST_STEP_STAGES", "25:60,50:60,100:60,200:60")
CLIENT_COUNTER = 0


def next_test_phone():
    global CLIENT_COUNTER
    CLIENT_COUNTER += 1
    return f"{CLIENT_PHONE_PREFIX}{CLIENT_COUNTER:04d}"


def parse_step_stages(raw_value: str):
    stages = []
    elapsed = 0
    for chunk in raw_value.split(","):
        chunk = chunk.strip()
        if not chunk:
            continue
        user_part, duration_part = chunk.split(":")
        users = int(user_part.strip())
        duration_seconds = int(duration_part.strip())
        elapsed += duration_seconds
        stages.append((elapsed, users))
    return stages


class BackendBaseUser(HttpUser):
    abstract = True
    wait_time = between(1, 3)

    club_id = None
    access_token = None
    refresh_token = None

    def auth_headers(self):
        if not self.access_token:
            return {}
        return {"Authorization": f"Bearer {self.access_token}"}

    def booking_window(self):
        start_at = datetime.now().replace(microsecond=0) + timedelta(hours=TIME_OFFSET_HOURS)
        end_at = start_at + timedelta(hours=TIME_WINDOW_HOURS)
        return start_at.isoformat(), end_at.isoformat()

    def ensure_club_id(self):
        if self.club_id is not None:
            return self.club_id
        if LOCUST_CLUB_ID:
            self.club_id = int(LOCUST_CLUB_ID)
            return self.club_id

        with self.client.get("/api/v1/clubs", name="/api/v1/clubs [discover]", catch_response=True) as response:
            if response.status_code != 200:
                response.failure(f"Cannot load clubs: {response.status_code}")
                raise StopUser()
            clubs = response.json()
            if not clubs:
                response.failure("No clubs available for load testing")
                raise StopUser()
            self.club_id = clubs[0]["id"]
        return self.club_id


class PublicApiUser(BackendBaseUser):
    weight = 3

    def on_start(self):
        self.ensure_club_id()

    @task(1)
    def ping(self):
        self.client.get("/api/v1/ping", name="/api/v1/ping")

    @task(3)
    def clubs(self):
        self.client.get("/api/v1/clubs", name="/api/v1/clubs")

    @task(2)
    def club_details(self):
        self.client.get(f"/api/v1/clubs/{self.ensure_club_id()}", name="/api/v1/clubs/{clubId}")

    @task(2)
    def product_categories(self):
        self.client.get("/api/v1/product-categories", name="/api/v1/product-categories")

    @task(2)
    def club_products(self):
        self.client.get(
            f"/api/v1/clubs/{self.ensure_club_id()}/products",
            name="/api/v1/clubs/{clubId}/products",
        )

    @task(2)
    def seat_prices(self):
        self.client.get(
            f"/api/v1/clubs/{self.ensure_club_id()}/seat-prices",
            name="/api/v1/clubs/{clubId}/seat-prices",
        )

    @task(2)
    def time_packages(self):
        self.client.get(
            f"/api/v1/clubs/{self.ensure_club_id()}/time-packages",
            name="/api/v1/clubs/{clubId}/time-packages",
        )

    @task(2)
    def seats(self):
        self.client.get(
            f"/api/v1/clubs/{self.ensure_club_id()}/seats",
            name="/api/v1/clubs/{clubId}/seats",
        )

    @task(2)
    def floorplan(self):
        self.client.get(
            f"/api/v1/clubs/{self.ensure_club_id()}/floorplan",
            name="/api/v1/clubs/{clubId}/floorplan",
        )

    @task(2)
    def floorplan_with_availability(self):
        start_at, end_at = self.booking_window()
        self.client.get(
            f"/api/v1/clubs/{self.ensure_club_id()}/floorplan-with-availability",
            params={"from": start_at, "to": end_at},
            name="/api/v1/clubs/{clubId}/floorplan-with-availability",
        )

    @task(2)
    def seat_availability(self):
        start_at, end_at = self.booking_window()
        self.client.post(
            f"/api/v1/clubs/{self.ensure_club_id()}/seats/availability",
            json={"startAt": start_at, "endAt": end_at},
            name="/api/v1/clubs/{clubId}/seats/availability",
        )

    @task(1)
    def max_availability(self):
        start_at, _ = self.booking_window()
        self.client.post(
            f"/api/v1/clubs/{self.ensure_club_id()}/seats/max-availability",
            json={"startAt": start_at},
            name="/api/v1/clubs/{clubId}/seats/max-availability",
        )


class AuthenticatedClientUser(BackendBaseUser):
    weight = 1 if ENABLE_CLIENT_AUTH else 0

    def on_start(self):
        self.ensure_club_id()
        self.login_via_otp()

    def login_via_otp(self):
        phone = CLIENT_PHONE or next_test_phone()
        with self.client.post(
            "/api/v1/auth/otp/request",
            json={"phone": phone},
            name="/api/v1/auth/otp/request",
            catch_response=True,
        ) as response:
            if response.status_code != 200:
                response.failure(f"OTP request failed: {response.status_code}")
                raise StopUser()
            payload = response.json()
            debug_code = payload.get("debugCode")
            if not debug_code:
                response.failure("Backend did not return debugCode for OTP flow")
                raise StopUser()
            challenge_id = payload["challengeId"]

        with self.client.post(
            "/api/v1/auth/otp/verify",
            json={"challengeId": challenge_id, "code": debug_code},
            name="/api/v1/auth/otp/verify",
            catch_response=True,
        ) as response:
            if response.status_code != 200:
                response.failure(f"OTP verify failed: {response.status_code}")
                raise StopUser()
            payload = response.json()
            self.access_token = payload["accessToken"]
            self.refresh_token = payload["refreshToken"]

    @task(2)
    def me(self):
        self.client.get("/api/v1/me", headers=self.auth_headers(), name="/api/v1/me")

    @task(2)
    def me_context(self):
        self.client.get("/api/v1/me/context", headers=self.auth_headers(), name="/api/v1/me/context")

    @task(2)
    def available_clubs(self):
        self.client.get("/api/v1/clubs/available", headers=self.auth_headers(), name="/api/v1/clubs/available")

    @task(2)
    def favorites(self):
        self.client.get("/api/v1/me/favorites", headers=self.auth_headers(), name="/api/v1/me/favorites")

    @task(1)
    def select_cart_club(self):
        self.client.put(
            "/api/v1/cart/club",
            headers=self.auth_headers(),
            json={"clubId": self.ensure_club_id()},
            name="/api/v1/cart/club",
        )

    @task(1)
    def get_cart(self):
        self.client.get(
            "/api/v1/cart",
            headers=self.auth_headers(),
            params={"clubId": self.ensure_club_id()},
            name="/api/v1/cart",
        )

    @task(1)
    def refresh_tokens(self):
        with self.client.post(
            "/api/v1/auth/refresh",
            json={"refreshToken": self.refresh_token},
            name="/api/v1/auth/refresh",
            catch_response=True,
        ) as response:
            if response.status_code != 200:
                response.failure(f"Client refresh failed: {response.status_code}")
                return
            payload = response.json()
            self.access_token = payload["accessToken"]
            self.refresh_token = payload["refreshToken"]

    @task(1)
    def public_catalog_as_user(self):
        self.client.get(
            f"/api/v1/clubs/{self.ensure_club_id()}/products",
            headers=self.auth_headers(),
            name="/api/v1/clubs/{clubId}/products [auth]",
        )

    @task(1)
    def manage_favorites(self):
        if not ENABLE_WRITE_TASKS:
            return
        club_id = self.ensure_club_id()
        self.client.put(
            f"/api/v1/me/favorites/{club_id}",
            headers=self.auth_headers(),
            name="/api/v1/me/favorites/{clubId} [put]",
        )
        self.client.delete(
            f"/api/v1/me/favorites/{club_id}",
            headers=self.auth_headers(),
            name="/api/v1/me/favorites/{clubId} [delete]",
        )

    @task(1)
    def submit_report(self):
        if not ENABLE_WRITE_TASKS:
            return
        message = f"Locust test report {random.randint(1000, 9999)}"
        self.client.post(
            f"/api/v1/clubs/{self.ensure_club_id()}/reports",
            headers=self.auth_headers(),
            json={"message": message},
            name="/api/v1/clubs/{clubId}/reports",
        )


class AdminUser(BackendBaseUser):
    weight = 1 if ADMIN_PHONE and ADMIN_PASSWORD else 0

    def on_start(self):
        self.login_admin()
        self.ensure_admin_club_id()

    def login_admin(self):
        with self.client.post(
            "/api/v1/admin/auth/login",
            json={"phone": ADMIN_PHONE, "password": ADMIN_PASSWORD},
            name="/api/v1/admin/auth/login",
            catch_response=True,
        ) as response:
            if response.status_code != 200:
                response.failure(f"Admin login failed: {response.status_code}")
                raise StopUser()
            payload = response.json()
            self.access_token = payload["accessToken"]
            self.refresh_token = payload["refreshToken"]


if USE_STEP_LOAD:
    class StepUserLoadShape(LoadTestShape):
        stages = parse_step_stages(STEP_STAGES_RAW)
        spawn_rate = STEP_SPAWN_RATE

        def tick(self):
            run_time = self.get_run_time()
            for stage_end, user_count in self.stages:
                if run_time < stage_end:
                    return user_count, self.spawn_rate
            return None

    def ensure_admin_club_id(self):
        if LOCUST_ADMIN_CLUB_ID:
            self.club_id = int(LOCUST_ADMIN_CLUB_ID)
            return
        with self.client.get(
            "/api/v1/me/context",
            headers=self.auth_headers(),
            name="/api/v1/me/context [admin]",
            catch_response=True,
        ) as response:
            if response.status_code != 200:
                response.failure(f"Cannot load admin context: {response.status_code}")
                raise StopUser()
            payload = response.json()
            clubs = payload.get("clubs") or []
            if not clubs:
                response.failure("Admin user has no club memberships")
                raise StopUser()
            self.club_id = clubs[0]["clubId"]

    @task(2)
    def dashboard(self):
        self.client.get(
            f"/api/v1/admin/clubs/{self.club_id}/dashboard",
            headers=self.auth_headers(),
            name="/api/v1/admin/clubs/{clubId}/dashboard",
        )

    @task(2)
    def bookings(self):
        start_at, end_at = self.booking_window()
        self.client.get(
            f"/api/v1/admin/clubs/{self.club_id}/bookings",
            headers=self.auth_headers(),
            params={"from": start_at, "to": end_at},
            name="/api/v1/admin/clubs/{clubId}/bookings",
        )

    @task(2)
    def purchases(self):
        start_at, end_at = self.booking_window()
        self.client.get(
            f"/api/v1/admin/clubs/{self.club_id}/purchases",
            headers=self.auth_headers(),
            params={"from": start_at, "to": end_at},
            name="/api/v1/admin/clubs/{clubId}/purchases",
        )

    @task(2)
    def user_reports(self):
        self.client.get(
            f"/api/v1/admin/clubs/{self.club_id}/user-reports",
            headers=self.auth_headers(),
            name="/api/v1/admin/clubs/{clubId}/user-reports",
        )

    @task(1)
    def platform_messages(self):
        self.client.get(
            f"/api/v1/admin/clubs/{self.club_id}/platform-messages",
            headers=self.auth_headers(),
            name="/api/v1/admin/clubs/{clubId}/platform-messages",
        )

    @task(1)
    def refresh_tokens(self):
        with self.client.post(
            "/api/v1/admin/auth/refresh",
            json={"refreshToken": self.refresh_token},
            name="/api/v1/admin/auth/refresh",
            catch_response=True,
        ) as response:
            if response.status_code != 200:
                response.failure(f"Admin refresh failed: {response.status_code}")
                return
            payload = response.json()
            self.access_token = payload["accessToken"]
            self.refresh_token = payload["refreshToken"]
