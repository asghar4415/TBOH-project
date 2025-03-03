package com.example.thebridgeofhopes.ui.theme.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.thebridgeofhopes.model.AIModel


@Composable
fun LearningScreen(navController: NavController) {
    val activity = LocalActivity.current
    LaunchedEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    var attemptsLeft by remember { mutableStateOf(5) }
    var score by remember { mutableStateOf(0) }
    val strokes = remember { mutableStateListOf<MutableList<Offset>>() } // Stores multiple strokes
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

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
            // Top row with back button and attempts
            Row(
                modifier = Modifier.fillMaxWidth(),
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

            Text(text = "Score: $score", fontSize = 24.sp, fontWeight = FontWeight.Bold)

            // White canvas area in the middle
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .fillMaxHeight(0.8f)
                    .background(Color.White, shape = RoundedCornerShape(16.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Slightly visible alphabet
                Text(
                    text = "A",
                    fontSize = 120.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.Gray.copy(alpha = 0.3f)
                )

                DrawingCanvas(strokes) { drawnBitmap ->
                    bitmap = drawnBitmap
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Check Button
            Button(onClick = {
                bitmap?.let { drawnBitmap ->
                    val result = AIModel.evaluateDrawing(drawnBitmap)
                    score = result
                    attemptsLeft -= 1
                    strokes.clear() // Reset the drawing
                }
            }) {
                Text(text = "Check")
            }
        }
    }
}

@Composable
fun DrawingCanvas(strokes: MutableList<MutableList<Offset>>, onBitmapCaptured: (Bitmap) -> Unit) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { strokes.add(mutableListOf()) }, // Add new stroke list
                    onDrag = { change, _ ->
                        strokes.last().add(change.position)
                    }
                )
            }
    ) {
        val path = Path().apply {
            strokes.forEach { stroke ->
                if (stroke.isNotEmpty()) {
                    moveTo(stroke.first().x, stroke.first().y)
                    stroke.drop(1).forEach { point -> lineTo(point.x, point.y) }
                }
            }
        }

        drawPath(
            path = path,
            color = Color.Black,
            style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }

    // Capture the final drawing when "Check" is clicked
    LaunchedEffect(strokes) {
        val bitmap = Bitmap.createBitmap(500, 300, Bitmap.Config.ARGB_8888).apply {
            val canvas = Canvas(this)
            val paint = Paint().apply {
                color = android.graphics.Color.BLACK
                style = Paint.Style.STROKE
                strokeWidth = 8f
                isAntiAlias = true
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            val path = android.graphics.Path().apply {
                strokes.forEach { stroke ->
                    if (stroke.isNotEmpty()) {
                        moveTo(stroke.first().x, stroke.first().y)
                        stroke.drop(1).forEach { point -> lineTo(point.x, point.y) }
                    }
                }
            }
            canvas.drawPath(path, paint)
        }
        onBitmapCaptured(bitmap)
    }
}
