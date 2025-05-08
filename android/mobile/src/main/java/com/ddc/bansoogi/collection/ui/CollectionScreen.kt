package com.ddc.bansoogi.collection.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ddc.bansoogi.collection.data.model.CollectionDto
import com.ddc.bansoogi.collection.controller.CollectionController
import com.ddc.bansoogi.collection.view.CollectionView
import com.ddc.bansoogi.R
import com.ddc.bansoogi.collection.data.entity.Character
import com.ddc.bansoogi.collection.data.local.CollectionDataSource
import com.ddc.bansoogi.common.data.local.RealmManager
import io.realm.kotlin.ext.query

@Preview
@Composable
fun CollectionScreen() {
    val collectionDtoState = remember { mutableStateListOf<CollectionDto>() }
    var selected by remember { mutableStateOf<CollectionDto?>(null) }

    val view =  object : CollectionView {
        override fun displayCollectionList(collectionDtoList: List<CollectionDto>) {
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
        CollectionDataSource().insertDummyCharactersWithUnlock()
        controller.initialize()

        val realm = RealmManager.realm
        val allCharacters = realm.query<Character>().find()
    }

    val regularList = collectionDtoState.filter { it.id < 50 }
    val hiddenList = collectionDtoState.filter { it.id >= 50 }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x90EEEEEE))
            .padding(horizontal = 12.dp),
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .background(Color.White, shape = RoundedCornerShape(16.dp))
                .border(width = 2.dp, color = Color.Black, shape = RoundedCornerShape(16.dp))
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.bansoogi_default_profile),
                    contentDescription = "내 컬렉션 아이콘",
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "내 컬렉션",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        SectionHeader("일반", R.drawable.egg_white)

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.heightIn(max = 300.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(regularList) { character ->
                CollectionGridItem(character, onClick = { if (character.isUnlocked) controller.onCharacterClick(character) })
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
                CollectionGridItem(character, onClick = { if (character.isUnlocked) controller.onCharacterClick(character) })
            }
        }

        selected?.let {
            CollectionDetailDialog(
                character = it,
                fullList = collectionDtoState,
                onDismiss = { controller.dismissCharacterDetail() }
            )
        }
    }
}

@Composable
fun SectionHeader(title: String, iconRes: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .wrapContentWidth()
            .padding(vertical = 12.dp)
            .background(
                color = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = title,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
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
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, Color.Gray, shape = RoundedCornerShape(12.dp))
                .background(Color.LightGray)
        ) {
            Text("이미지 없음", modifier = Modifier.align(Alignment.Center))
        }
        return
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, Color.Gray, shape = RoundedCornerShape(12.dp))
            .clickable(enabled = character.isUnlocked, onClick = onClick)
    ) {
        Image(
            painter = painterResource(id = imageResId),
            contentDescription = character.title,
            modifier = Modifier.fillMaxSize()
        )
    }
}