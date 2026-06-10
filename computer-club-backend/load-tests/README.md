# Locust load tests for backend

This directory contains a basic Locust scenario for the backend of the
Computer Club application.

## What is covered

- public catalog endpoints
- seat availability and floorplan endpoints
- optional client OTP flow and authenticated client endpoints
- optional admin login and reporting endpoints

## Recommended diploma format

For the thesis, it is better to run several separate tests with increasing
number of concurrent users and then compare the results. This makes it easy to
show how server response time and throughput change under load.

Recommended sequence:

- `25` users
- `50` users
- `100` users
- `200` users

Recommended duration of each run:

- `2` minutes

## Quick start

1. Start PostgreSQL and Redis.
2. Start the backend on `http://localhost:8080`.
3. Install Locust:

```powershell
pip install -r .\load-tests\requirements.txt
```

or locally into the project:

```powershell
& 'C:\Users\geenb\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe' -m pip install -r .\load-tests\requirements.txt --target .\load-tests\.deps
```

4. Run Locust:

```powershell
locust -f .\load-tests\locustfile.py -H http://localhost:8080
```

5. Open the Locust web UI:

`http://localhost:8089`

## Live step-load test in Locust UI

If you want to see directly in Locust how the number of users affects response
time and throughput, run the test in step-load mode:

```powershell
$env:LOCUST_USE_STEP_LOAD = '1'
$env:LOCUST_STEP_STAGES = '25:60,50:60,100:60,200:60'
$env:LOCUST_STEP_SPAWN_RATE = '5'
.\load-tests\.venv\Scripts\locust.exe -f .\load-tests\locustfile.py -H http://localhost:8080
```

Meaning of `LOCUST_STEP_STAGES`:

- `25:60` - 25 users for 60 seconds
- `50:60` - 50 users for 60 seconds
- `100:60` - 100 users for 60 seconds
- `200:60` - 200 users for 60 seconds

After startup:

1. open `http://localhost:8089`
2. press `Start`
3. Locust will increase the number of users automatically by stages
4. watch the charts `Response Times`, `Requests/s`, `Failures` and `Number of Users`

This is the best way to make a screenshot for the diploma directly from Locust.

## Automatic matrix run

To compare the effect of increasing load, use the prepared script:

```powershell
.\load-tests\run-load-matrix.ps1
```

By default it runs:

- `25`, `50`, `100`, `200` users
- spawn rate `5` users per second
- run time `2m` for each level

You can override parameters:

```powershell
.\load-tests\run-load-matrix.ps1 -Users 50,100,200,300 -SpawnRate 10 -RunTime 3m
```

After execution, the script creates:

- separate HTML reports for each load level
- CSV statistics for each run
- a combined table `summary.csv`
- a combined markdown report `summary.md`
- a comparison chart `summary.svg`

## Environment variables

Optional variables:

- `LOCUST_CLUB_ID` - fixed club id; if omitted, the first club is loaded from `/api/v1/clubs`
- `LOCUST_ENABLE_CLIENT_AUTH` - `1` to enable OTP client flow
- `LOCUST_CLIENT_PHONE` - fixed phone number for OTP auth flow
- `LOCUST_CLIENT_PHONE_PREFIX` - prefix for generated test phones, default `+7999000`
- `LOCUST_ADMIN_PHONE` - admin phone for `/api/v1/admin/auth/login`
- `LOCUST_ADMIN_PASSWORD` - admin password
- `LOCUST_ADMIN_CLUB_ID` - club id for admin endpoints; if omitted, the first club from `/api/v1/me/context` is used
- `LOCUST_ENABLE_WRITE_TASKS` - `1` to enable report and favorite write operations
- `LOCUST_TIME_OFFSET_HOURS` - how far in the future the booking window starts, default `2`
- `LOCUST_TIME_WINDOW_HOURS` - booking window duration, default `2`

Example:

```powershell
$env:LOCUST_ENABLE_CLIENT_AUTH = '1'
$env:LOCUST_ADMIN_PHONE = '+79991112233'
$env:LOCUST_ADMIN_PASSWORD = 'secret123'
locust -f .\load-tests\locustfile.py -H http://localhost:8080
```

If OTP debug mode is enabled on the backend, generated phone numbers are enough for the
client flow. This avoids collisions between virtual users during OTP request and verify.

## What to capture for the diploma

The most useful materials for the diploma are:

- the comparison chart `summary.svg`
- the summary table from `summary.md`
- one Locust HTML report for the highest load level

If you need only one visual figure, use `summary.svg`. It is the clearest way to
show how increasing the number of users affects average response time, P95 response
time, throughput, and error rate.
