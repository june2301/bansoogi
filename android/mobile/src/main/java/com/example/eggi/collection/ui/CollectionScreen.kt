package com.example.eggi.collection.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.eggi.collection.data.model.CollectionDto
import com.example.eggi.collection.controller.CollectionController
import com.example.eggi.collection.view.CollectionView
import com.example.eggi.R
import coil.compose.AsyncImage
import com.example.eggi.collection.data.entity.Character
import com.example.eggi.common.data.local.RealmManager
import io.realm.kotlin.ext.query

@Preview
@Composable
fun CollectionScreen() {
    val collectionDtoState = remember { mutableStateListOf<CollectionDto>() }
    var selected by remember { mutableStateOf<CollectionDto?>(null) }

    val view =  object : CollectionView {
        override fun displayCollectionList(collectionDtoList: List<CollectionDto>) {
            println("🔥 불러온 캐릭터 개수: ${collectionDtoList.size}")
            collectionDtoState.clear()
            collectionDtoState.addAll(collectionDtoList)
        }

        override fun showCharacterDetail(character: CollectionDto) {
            selected = character
        }

        override fun dismissCharacterDetail() {
            selected = null
        }
    }

    val controller = remember { CollectionController(view) }

    LaunchedEffect(Unit) {
        insertDummyCharacters()
        controller.initialize()
    }

    val regularList = collectionDtoState.filter { it.id < 50 }
    val hiddenList = collectionDtoState.filter { it.id >= 50 }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "🐣 내 컬렉션",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        SectionHeader("일반", R.drawable.egg_white)

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.heightIn(max = 300.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(regularList) { character ->
                CollectionGridItem(character, onClick = { controller.onCharacterClick(character) })
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SectionHeader("히든", R.drawable.egg_gold)

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.heightIn(max = 300.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(hiddenList) { character ->
                CollectionGridItem(character, onClick = { controller.onCharacterClick(character) })
            }
        }

        selected?.let {
            CollectionDetailDialog(character = it) {
                controller.dismissCharacterDetail()
            }
        }
    }
}

suspend fun insertDummyCharacters() {
    val realm = RealmManager.realm

    val alreadyExists = realm.query<Character>().find().isNotEmpty()
    if (alreadyExists) return

    val dummyList = listOf(
        Character().apply {
            bansoogiId = 1
            title = "기본 반숙"
            imageUrl = "bansoogi_default_profile"
            silhouetteImageUrl = "unknown"
            gifUrl = "bansoogi_basic"
            description = "가장 처음 등장하는 반숙이입니다."
        }
    )

    realm.write {
        dummyList.forEach { copyToRealm(it) }
    }
}

@Composable
fun SectionHeader(title: String, iconRes: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = title,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
fun CollectionGridItem(character: CollectionDto, onClick: () -> Unit) {
    val context = LocalContext.current
    val imageName = if (character.isUnlocked) character.imageUrl else character.silhouetteImageUrl

    val imageResId = remember(imageName) {
        context.resources.getIdentifier(imageName, "drawable", context.packageName)
    }

    if (imageResId == 0) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .background(Color.LightGray)
        ) {
            Text("이미지 없음", modifier = Modifier.align(Alignment.Center))
        }
        return
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(enabled = character.isUnlocked, onClick = onClick)
    ) {
        Image(
            painter = painterResource(id = imageResId),
            contentDescription = character.title,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun CollectionDetailDialog(character: CollectionDto, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("닫기") }
        },
        title = { Text(text = character.title) },
        text = { Text(text = character.description) },
        icon = {
            AsyncImage(
                model = character.imageUrl,
                contentDescription = character.title,
                modifier = Modifier.size(96.dp)
            )
        }
    )
}