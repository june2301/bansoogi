package com.ddc.bansoogi.main.ui.egg

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ddc.bansoogi.R
import com.ddc.bansoogi.collection.data.entity.Character
import com.ddc.bansoogi.collection.data.entity.UnlockedCharacter
import com.ddc.bansoogi.collection.data.local.CollectionDataSource
import com.ddc.bansoogi.collection.util.CharacterPicker
import com.ddc.bansoogi.common.data.local.RealmManager
import com.ddc.bansoogi.common.util.SpriteSheetAnimation
import io.realm.kotlin.ext.query
import io.realm.kotlin.types.RealmInstant
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CharacterGetScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentStage by remember { mutableStateOf(0) } // 0: 빛남+펑, 1: 캐릭터 등장
    var selectedCharacter by remember { mutableStateOf<Character?>(null) }

    LaunchedEffect(Unit) {
        val characters = CollectionDataSource().getAllBansoogi()
        characters.collect { list ->
            selectedCharacter = CharacterPicker.pickRandomBansoogi(list)
        }
    }

    // 애니메이션 단계 진행
    LaunchedEffect(selectedCharacter) {
        if (selectedCharacter != null) {
            delay(2000) // 빛남+펑 sprite 이후
            currentStage = 1 // 캐릭터 등장

            // 획득 처리
            val realm = RealmManager.realm
            val bansoogiId = selectedCharacter!!.bansoogiId
            realm.write {
                val existing = query<UnlockedCharacter>("bansoogiId == $0", bansoogiId).first().find()
                if (existing != null) {
                    findLatest(existing)?.apply {
                        acquisitionCount += 1
                        updatedAt = RealmInstant.now()
                    }
                } else {
                    copyToRealm(
                        UnlockedCharacter().apply {
                            this.bansoogiId = bansoogiId
                            this.acquisitionCount = 1
                            this.createdAt = RealmInstant.now()
                            this.updatedAt = RealmInstant.now()
                        }
                    )
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(enabled = currentStage == 1) { navController.popBackStack() },
        contentAlignment = Alignment.Center
    ) {
        // 배경
        Image(
            painter = painterResource(id = R.drawable.background_kitchen_gif),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )

        if (currentStage == 0) {
            SpriteSheetAnimation(
                context = context,
                spriteSheetName = "bansoogi_explode.png",
                jsonName = "bansoogi_explode.json",
                modifier = Modifier.size(180.dp)
            )
        }

        if (currentStage == 1 && selectedCharacter != null) {
            SpriteSheetAnimation(
                context = context,
                spriteSheetName = "${selectedCharacter!!.gifUrl}.png",
                jsonName = "${selectedCharacter!!.imageUrl}.json",
                modifier = Modifier.size(128.dp)
            )
        }
    }
}