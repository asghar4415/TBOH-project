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





@Composable
fun DigitsLearn(navController: NavController) {
    val context = LocalContext.current
    val activity = context as? Activity

    LaunchedEffect(Unit) {
        AIModel.loadModel2(context)
    }

    var attemptsLeft by remember { mutableIntStateOf(5) }
    val score by remember { mutableIntStateOf(0) }
    val lines = remember { mutableStateListOf<Line>() }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    var showDialog by remember { mutableStateOf(false) }
    var dialogScore by remember { mutableStateOf<Int?>(null) }
    var showGameOverDialog by remember { mutableStateOf(false) }
    var selectedDigit by remember { mutableStateOf("0") } // Changed from selectedLetter
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

        // Changed text to reference digits instead of letters
        Text(
            text = "Digit: $selectedDigit, Score: $score",
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
            Text(selectedDigit, fontSize = 22.sp, color = Color.White)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            // Changed to show digits 0-9 instead of letters
            ('0'..'9').forEach { digit ->
                DropdownMenuItem(
                    text = { Text(digit.toString(), fontSize = 18.sp, color = Color.Black) },
                    onClick = {
                        selectedDigit = digit.toString()
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
                text = selectedDigit,
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

                        predictions = AIModel.evaluateDrawing2(context, bitmap)
                        Log.d("predictions", predictions.toString())
                        dialogScore = predictions.find { it.first.toString() == selectedDigit }?.second?.toInt() ?: 0
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
            // Sort predictions by digit (0-9) before displaying
            predictions.sortedBy { it.first }.forEach { (digit, score) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            "$digit: ${score.toInt()}",
                            fontSize = 18.sp,
                            color = Color.Black,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
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