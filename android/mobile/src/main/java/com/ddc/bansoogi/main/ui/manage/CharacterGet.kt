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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ddc.bansoogi.R
import com.ddc.bansoogi.calendar.controller.RecordedController
import com.ddc.bansoogi.collection.data.entity.Character
import com.ddc.bansoogi.common.data.model.TodayRecordDto
import com.ddc.bansoogi.common.navigation.NavRoutes
import com.ddc.bansoogi.common.ui.component.SpriteSheetAnimation
import com.ddc.bansoogi.main.controller.CharacterGetController
import com.ddc.bansoogi.main.controller.TodayRecordController
import com.ddc.bansoogi.main.view.TodayRecordView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.mongodb.kbson.ObjectId

@Composable
fun CharacterGetScreen(
    navController: NavController,
    walk: Int,
    stairs: Int,
    sleep: Int,
    exercise: Int
) {
    val context = LocalContext.current
    val controller = remember { CharacterGetController() }
    val todayRecordController = remember {
        TodayRecordController(object : TodayRecordView {
            override fun displayTodayRecord(todayRecord: TodayRecordDto) {}
            override fun showEmptyState() {}
        }, context = context)
    }

    var currentStage by remember { mutableIntStateOf(0) }
    var selectedCharacter by remember { mutableStateOf<Character?>(null) }
    var canDraw by remember { mutableStateOf(true) }
    var isViewedAlready by remember { mutableStateOf(true) }
    var todayRecordId by remember { mutableStateOf<ObjectId?>(null) }
    var isAnimationStarted by remember { mutableStateOf(false) }

    // 에너지 체크
    LaunchedEffect(Unit) {
        val result = withContext(Dispatchers.IO) {
            controller.canDrawCharacter()
        }
        canDraw = result
        if (!result) {
            navController.popBackStack()
        }
    }

    // isViewed 상태 조회
    var todayRecord by remember { mutableStateOf<TodayRecordDto?>(null) }
    LaunchedEffect(Unit) {
        todayRecord = todayRecordController.getTodayRecordSync()
        isViewedAlready = todayRecord?.isViewed ?: true
        todayRecordId = todayRecord?.recordId
    }

    // 캐릭터 뽑기
    LaunchedEffect(canDraw) {
        if (!canDraw) return@LaunchedEffect
        controller.getRandomBansoogi()
            .collect { character -> selectedCharacter = character }
    }

    // 저장 처리
    LaunchedEffect(isAnimationStarted) {
        if (isAnimationStarted && selectedCharacter != null) {
            delay(1250)
            currentStage = 1
            
            withContext(Dispatchers.IO) {
                // 랜덤 캐릭터 저장
                controller.saveUnlockedCharacter(selectedCharacter!!)
                
                // close 처리
                todayRecordController.updateIsClosed()
                
                // 캘린더에 저장
                RecordedController().createRecordedReport(
                    todayRecord!!,
                    selectedCharacter!!.bansoogiId,
                    walk,
                    stairs,
                    sleep,
                    exercise
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable {
                when {
                    !isViewedAlready && !isAnimationStarted -> {
                        isAnimationStarted = true
                    }
                    currentStage == 1 -> {
                        if (!isViewedAlready) {
                            todayRecordController.updateIsViewed(todayRecordId!!, true)
                        }
                        navController.navigate(NavRoutes.COLLECTION) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            }
        ,
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.background_kitchen),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 64.dp)
        ) {
            when {
                !isViewedAlready && !isAnimationStarted -> {
                    SpriteSheetAnimation(
                        context = context,
                        spriteSheetName = "bansoogi_basic_sheet.png",
                        jsonName = "bansoogi_default_profile.json",
                        modifier = Modifier.size(180.dp).scale(1.8f)
                    )
                }
                !isViewedAlready && currentStage == 0 -> {
                    SpriteSheetAnimation(
                        context = context,
                        spriteSheetName = "bansoogi_explode.png",
                        jsonName = "bansoogi_explode.json",
                        modifier = Modifier.size(180.dp).scale(1.8f)
                    )
                }
                currentStage == 1 && selectedCharacter != null -> {
                    SpriteSheetAnimation(
                        context = context,
                        spriteSheetName = "${selectedCharacter!!.gifUrl}_sheet.png",
                        jsonName = "${selectedCharacter!!.imageUrl}.json",
                        modifier = Modifier.size(180.dp).scale(1.8f)
                    )
                }
            }
        }

        when {
            !isViewedAlready && !isAnimationStarted -> {
                Text(
                    text = "터치해서 반숙이 변신",
                    color = Color.White,
                    fontSize = 20.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp)
                )
            }
            currentStage == 1 -> {
                Text(
                    text = "터치해서 컬렉션 확인",
                    color = Color.White,
                    fontSize = 20.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp)
                )
            }
        }
    }
}