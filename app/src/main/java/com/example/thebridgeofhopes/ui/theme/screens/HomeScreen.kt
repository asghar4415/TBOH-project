package com.example.thebridgeofhopes.ui.theme.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.thebridgeofhopes.R


@Composable
fun HomeScreen(navController: NavController) {
    var isClicked by remember { mutableStateOf(false) }
    var isHovered by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isClicked) 1.1f else 1f,
        animationSpec = tween(durationMillis = 200), label = ""
    )

    val buttonColor by animateColorAsState(
        targetValue = if (isHovered) Color(0xFF5EA34A) else Color(0xFF6BBF59),
        animationSpec = tween(durationMillis = 200), label = ""
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF99D993)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp)
                .padding(horizontal = 30.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Hello !!",
                fontSize = 35.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = Color.White,
                modifier = Modifier.align(Alignment.Start)
            )
            Text(
                text = "How are you today?",
                fontStyle = FontStyle.Italic,
                fontSize = 16.sp,
                color = Color.White,
                modifier = Modifier.align(Alignment.Start)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            Image(
                painter = painterResource(id = R.drawable.smiley2),
                contentDescription = "Smiley Logo",
                modifier = Modifier.size(190.dp)
            )

            Spacer(modifier = Modifier.height(70.dp))

            Button(
                onClick = {
                    isClicked = true
                    navController?.navigate("learn")
                },
                modifier = Modifier
                    .scale(scale)
                    .padding(horizontal = 20.dp)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                isHovered = event.changes.any { it.pressed }
                            }
                        }
                    },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Let's Start Learning",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
