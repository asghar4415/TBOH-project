package com.example.thebridgeofhopes.ui.theme.screens

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.navigation.NavController
import java.io.File
import java.io.FileOutputStream

@Composable
fun LearningScreen(navController: NavController) {
    val context = LocalContext.current
    val activity = context as? Activity

    // Set the screen orientation to landscape
    LaunchedEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    var attemptsLeft by remember { mutableStateOf(5) }
    var score by remember { mutableStateOf(0) }
    val lines = remember { mutableStateListOf<Line>() }
    var showDialog by remember { mutableStateOf(false) }
    var savedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }

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
            Row(
                modifier = Modifier.fillMaxWidth()
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

            Text(text = "Score: $score", fontSize = 24.sp, fontWeight = FontWeight.Bold)

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

                // DrawingCanvas now fills the entire Box area
                DrawingCanvas(lines = lines, modifier = Modifier.matchParentSize())
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row {
                Button(onClick = {
                    if (lines.isNotEmpty() && boxSize.width > 0 && boxSize.height > 0) {
                        // Create bitmap with same size as the white area
                        val bitmap = createBitmapFromLines(lines, boxSize)
                        savedBitmap = bitmap
                        showDialog = true
                        attemptsLeft -= 1
                        lines.clear()
                    }
                }) {
                    Text(text = "Check")
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(onClick = {
                    lines.clear()
                }) {
                    Text(text = "Clear Drawing")
                }
            }
        }
    }

    if (showDialog) {
        SaveImageDialog(
            bitmap = savedBitmap,
            onDismiss = {
                showDialog = false
                savedBitmap = null
            },
            onImageSaved = {
                saveBitmapToExternalStorage(context, it)
            }
        )
    }
}

@Composable
fun SaveImageDialog(
    bitmap: Bitmap?,
    onDismiss: () -> Unit,
    onImageSaved: (Bitmap) -> Unit
) {
    if (bitmap == null) return

    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000)
        isLoading = false
        onImageSaved(bitmap)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Saving Image") },
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
                Text("Image saved successfully!")
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

@Composable
fun DrawingCanvas(
    lines: MutableList<Line>,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    // Capture the canvas size from onGloballyPositioned
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

fun saveBitmapToExternalStorage(context: Context, bitmap: Bitmap) {
    val filename = "traced_image_${System.currentTimeMillis()}.png"
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }
            val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            imageUri?.let { uri ->
                resolver.openOutputStream(uri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }
            }
            Log.d("SaveImage", "Image saved successfully in external storage (MediaStore)")
        } else {
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val image = File(imagesDir, filename)
            FileOutputStream(image).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }
            Log.d("SaveImage", "Image saved successfully at: ${image.absolutePath}")
        }
    } catch (e: Exception) {
        Log.e("SaveImage", "Error saving image", e)
    }
}

data class Line(
    val start: Offset,
    val end: Offset,
    val color: Color = Color.Black,
    val strokeWidth: Dp = 3.dp
)
