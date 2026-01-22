package com.example.computerclub.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.computerclub.data.FakeData
import com.example.computerclub.vm.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClubDetailsScreen(
    clubId: String,
    appVm: AppViewModel,
    onChosen: () -> Unit,
    onBack: () -> Unit
) {
    val club = FakeData.clubs.firstOrNull { it.id == clubId }

    if (club == null) {
        Text("Клуб не найден")
        return
    }

    val isFav = appVm.isFavoriteClub(club.id)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(club.name) }, // ← вместо слова "Клуб" показываем название
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { appVm.toggleFavoriteClub(club.id) }) {
                        Icon(
                            imageVector = if (isFav) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Избранное"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(club.location, style = MaterialTheme.typography.labelLarge)
                    Text(club.address)
                    Divider()
                    Text(club.description)
                }
            }

            Spacer(Modifier.weight(1f))

            androidx.compose.material3.Button(
                onClick = {
                    appVm.chooseClub(club.id)
                    onChosen()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Выбрать")
            }
        }
    }
}
