package com.example.thebridgeofhopes.ui.theme.screens

import android.app.Activity
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
    val strokeWidth: Dp = 5.dp
)

@Composable
fun LearningScreen(navController: NavController) {
    val context = LocalContext.current
    val activity = context as? Activity

    LaunchedEffect(Unit) {
        AIModel.loadModel(context)
    }

    var attemptsLeft by remember { mutableIntStateOf(5) }
    val score by remember { mutableIntStateOf(0) }
    val lines = remember { mutableStateListOf<Line>() }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    var showDialog by remember { mutableStateOf(false) }
    var dialogScore by remember { mutableStateOf<Int?>(null) }
    var showGameOverDialog by remember { mutableStateOf(false) }
    var selectedLetter by remember { mutableStateOf("A") }
    var expanded by remember { mutableStateOf(false) }
    var predictions by remember { mutableStateOf<List<Pair<Char, Float>>>(emptyList()) }
    var predictionsExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF99D993))
            .padding(vertical = 24.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(10.dp)
                .padding(top = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                navController.popBackStack()
            }) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color(0xFF4A5A7F),
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                text = "❤️ $attemptsLeft",
                fontSize = 22.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "Letter: $selectedLetter , Score: $score",
            fontSize = 24.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = { expanded = true },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B9867))
        ) {
            Text(selectedLetter, fontSize = 22.sp, color = Color.White)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            ('A'..'Z').forEach { letter ->
                DropdownMenuItem(
                    text = { Text(letter.toString(), fontSize = 18.sp, color = Color.Black) },
                    onClick = {
                        selectedLetter = letter.toString()
                        expanded = false
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .aspectRatio(1f)
                .background(Color.White, shape = RoundedCornerShape(16.dp))
                .padding(16.dp)
                .onGloballyPositioned { coordinates -> boxSize = coordinates.size },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = selectedLetter,
                fontSize = 160.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray.copy(alpha = 0.3f)
            )
            DrawingCanvas(lines = lines, modifier = Modifier.matchParentSize())
        }

        Spacer(modifier = Modifier.height(30.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    if (lines.isNotEmpty() && boxSize.width > 0 && boxSize.height > 0) {
                        val bitmap = createBitmapFromLines(lines, boxSize)
                        predictions = AIModel.evaluateDrawing(context, bitmap)
                        dialogScore = predictions.find { it.first.toString() == selectedLetter }?.second?.toInt() ?: 0
                        showDialog = true
                        attemptsLeft--
                        lines.clear()
                        if (attemptsLeft == 0) {
                            showGameOverDialog = true
                            showDialog = false
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B9867))
            ) {
                Text(text = "Check", fontSize = 18.sp, color = Color.White)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Button(
                onClick = { lines.clear() },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B9867))
            ) {
                Text(text = "Clear", fontSize = 18.sp, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Dropdown to show AI predictions
        Button(
            onClick = { predictionsExpanded = true },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B9867))
        ) {
            Text("View Predictions", fontSize = 18.sp, color = Color.White)
        }

        DropdownMenu(
            expanded = predictionsExpanded,
            onDismissRequest = { predictionsExpanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            predictions.forEach { (letter, score) ->
                DropdownMenuItem(
                    text = { Text("$letter: ${score.toInt()}", fontSize = 18.sp, color = Color.Black) },
                    onClick = { predictionsExpanded = false }
                )
            }
        }

}

    if (showDialog && !showGameOverDialog) {
        ScoreDialog(score = dialogScore, onDismiss = { showDialog = false })
    }


    if (showGameOverDialog) {
        GameOverDialog(onDismiss = {
            showGameOverDialog = false
            attemptsLeft = 5
        })
    }
}

@Composable
fun ScoreDialog(
    score: Int?,
    onDismiss: () -> Unit
) {
    if (score == null) return

    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(2000)
        isLoading = false
    }

    val resultText = when (score) {
        in 0..20 -> "Bad"
        in 21..50 -> "Good"
        in 51..75 -> "Better"
        else -> "Best"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Checking Result: ") },
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
                Text("Your drawing is: $resultText")
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
                                strokeWidth = 5.dp
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
        strokeWidth = 15f
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
                    Text("Oh my god! You have no lives left!", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Red)
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
