package com.ddc.bansoogi.main.ui.manage

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ddc.bansoogi.R
import com.ddc.bansoogi.collection.data.entity.Character
import com.ddc.bansoogi.common.util.SpriteSheetAnimation
import com.ddc.bansoogi.main.controller.CharacterGetController
import kotlinx.coroutines.delay

@Composable
fun CharacterGetScreen(navController: NavController) {
    val context = LocalContext.current
    val controller = remember { CharacterGetController() }

    var currentStage by remember { mutableIntStateOf(0) }
    var selectedCharacter by remember { mutableStateOf<Character?>(null) }
    var canDraw by remember { mutableStateOf(true) }

    // 에너지 체크
    LaunchedEffect(Unit) {
        canDraw = controller.canDrawCharacter()
        if (!canDraw) {
            navController.popBackStack()
        }
    }

    // 캐릭터 뽑기
    LaunchedEffect(canDraw) {
        if (canDraw) {
            controller.getRandomBansoogi().collect { character ->
                selectedCharacter = character
            }
        }
    }

    // 저장 처리
    LaunchedEffect(selectedCharacter) {
        if (selectedCharacter != null) {
            delay(1250)
            currentStage = 1
            controller.saveUnlockedCharacter(selectedCharacter!!)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(enabled = currentStage == 1) {
                navController.navigate("collection")
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.background_kitchen),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        if (currentStage == 0) {
            Box(modifier = Modifier.padding(top = 64.dp)) {
                SpriteSheetAnimation(
                    context = context,
                    spriteSheetName = "bansoogi_explode.png",
                    jsonName = "bansoogi_explode.json",
                    modifier = Modifier.size(180.dp)
                )
            }
        }

        if (currentStage == 1 && selectedCharacter != null) {
            Box(
                modifier = Modifier.padding(top = 64.dp),
                contentAlignment = Alignment.Center
            ) {
                SpriteSheetAnimation(
                    context = context,
                    spriteSheetName = "${selectedCharacter!!.gifUrl}_sheet.png",
                    jsonName = "${selectedCharacter!!.imageUrl}.json",
                    modifier = Modifier.size(180.dp)
                )
                Text(
                    text = "터치해서 컬렉션 확인",
                    color = Color.White,
                    fontSize = 20.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(top = 20.dp)
                )
            }
        }
    }
}