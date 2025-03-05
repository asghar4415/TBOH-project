package com.example.thebridgeofhopes.ui.theme.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import kotlinx.coroutines.delay


data class Line(
    val start: Offset,
    val end: Offset,
    val color: Color = Color.Black,
    val strokeWidth: Dp = 3.dp
)

/**
 * The main screen where the user traces the letter.
 */
@Composable
fun LearningScreen(navController: NavController) {
    val context = LocalContext.current
    val activity = context as? Activity

    // 1) Load the model once at startup
    LaunchedEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        AIModel.loadModel(context)  // <--- Load your TFLite model
    }

    var attemptsLeft by remember { mutableStateOf(5) }
    var score by remember { mutableStateOf(0) }
    val lines = remember { mutableStateListOf<Line>() }

    // For the final bitmap and a possible "score" or status
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    var showDialog by remember { mutableStateOf(false) }
    var dialogScore by remember { mutableStateOf<Int?>(null) }
    var showGameOverDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFE0E0))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = {
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    navController.popBackStack()
                }) {
                    Text("❌", fontSize = 24.sp)
                }
                Text(
                    text = "❤️ $attemptsLeft",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Score display
            Text(text = "Score: $score", fontSize = 24.sp, fontWeight = FontWeight.Bold)

            // White box for tracing
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .fillMaxHeight(0.8f)
                    .background(Color.White, shape = RoundedCornerShape(16.dp))
                    .padding(16.dp)
                    .onGloballyPositioned { coordinates ->
                        boxSize = coordinates.size
                    },
                contentAlignment = Alignment.Center
            ) {
                // The underlying alphabet text
                Text(
                    text = "A",
                    fontSize = 140.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.Gray.copy(alpha = 0.3f)
                )

                // Canvas for user drawing
                DrawingCanvas(lines = lines, modifier = Modifier.matchParentSize())
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Buttons
            Row {
                // "Check" button
                Button(onClick = {
                    if (lines.isNotEmpty() && boxSize.width > 0 && boxSize.height > 0) {
                        val bitmap = createBitmapFromLines(lines, boxSize)
                        val localScore = AIModel.evaluateDrawing(bitmap)

                        score = localScore
                        dialogScore = localScore
                        showDialog = true
                        attemptsLeft -= 1
                        lines.clear()

                        // Show game-over dialog if lives reach zero
                        if (attemptsLeft == 0) {
                            showGameOverDialog = true
                        }
                    }
                }) {
                    Text(text = "Check")
                }

                if (showGameOverDialog) {
                    GameOverDialog(onDismiss = {
                        showGameOverDialog = false
                        attemptsLeft = 5  // Reset lives (or navigate to a new screen)
                    })
                }

                Spacer(modifier = Modifier.width(16.dp))

                // "Clear Drawing" button
                Button(onClick = {
                    lines.clear()
                }) {
                    Text(text = "Clear Drawing")
                }
            }
        }
    }

    // Show a dialog with the result
    if (showDialog) {
        ScoreDialog(
            score = dialogScore,
            onDismiss = { showDialog = false }
        )
    }
}

/**
 * The dialog that displays "Processing..." for 2 seconds,
 * then shows the score from the model.
 */
@Composable
fun ScoreDialog(
    score: Int?,
    onDismiss: () -> Unit
) {
    if (score == null) return

    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        // Show "Processing..." for 2 seconds
        delay(2000)
        isLoading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI Model Evaluation") },
        text = {
            if (isLoading) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Processing...", fontSize = 16.sp)
                }
            } else {
                // Show the final score
                Text("Your score is: $score")
            }
        },
        confirmButton = {
            if (!isLoading) {
                Button(onClick = onDismiss) {
                    Text("OK")
                }
            }
        }
    )
}

/**
 * Canvas composable for user drawing input.
 */
@Composable
fun DrawingCanvas(
    lines: MutableList<Line>,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    Canvas(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                canvasSize = coordinates.size.toSize()
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val newStart = change.position - dragAmount
                    val newEnd = change.position

                    // Restrict lines to the canvas boundaries
                    if (newStart.x in 0f..canvasSize.width &&
                        newStart.y in 0f..canvasSize.height &&
                        newEnd.x in 0f..canvasSize.width &&
                        newEnd.y in 0f..canvasSize.height
                    ) {
                        lines.add(
                            Line(
                                start = newStart,
                                end = newEnd,
                                strokeWidth = 3.dp
                            )
                        )
                    }
                }
            }
    ) {
        lines.forEach { line ->
            drawLine(
                color = line.color,
                start = line.start,
                end = line.end,
                strokeWidth = with(density) { line.strokeWidth.toPx() },
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Convert the drawn lines to a Bitmap matching the size of the Box.
 */
fun createBitmapFromLines(lines: List<Line>, size: IntSize): Bitmap {
    val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)

    val paint = Paint().apply {
        color = android.graphics.Color.BLACK
        strokeWidth = 10f
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    lines.forEach { line ->
        canvas.drawLine(line.start.x, line.start.y, line.end.x, line.end.y, paint)
    }

    return bitmap
}

@Composable
fun GameOverDialog(onDismiss: () -> Unit) {
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(2000)
        isLoading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Game Over!", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Red) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Processing...", fontSize = 16.sp)
                } else {
                    Text("😱 Oh my god! You have no lives left!", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                }
            }
        },
        confirmButton = {
            if (!isLoading) {
                Button(onClick = onDismiss) {
                    Text("Restart")
                }
            }
        }
    )
}
