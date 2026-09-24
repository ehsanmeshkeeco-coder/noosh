package com.example.presentation.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.clerk.AuthState
import com.example.presentation.components.NooshCharacterView
import com.example.presentation.theme.NooshPrimary
import com.example.presentation.theme.NooshSubtleBlue
import com.example.presentation.viewmodel.MainViewModel

@Composable
fun AuthScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onAuthSuccess: () -> Unit = onNavigateBack,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsState()

    var nameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .testTag("auth_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Back Button (Only visible if user can go back or is logged in)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "بازگشت",
                    tint = Color(0xFF475569)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        NooshCharacterView(size = 90.dp)

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "ورود و ثبت‌نام در نوش",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF0F172A),
            textAlign = TextAlign.Center
        )

        Text(
            text = "با ثبت اطلاعات، سوابق مصرف آب و همراه سلامت شما همیشه محفوظ می‌ماند",
            fontSize = 13.sp,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        when (val state = authState) {
            is AuthState.Authenticated -> {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(NooshSubtleBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = NooshPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = state.user.firstName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )

                        Text(
                            text = state.user.email,
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "شناسه Clerk: ${state.user.id}",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        val statusLabel = if (state.user.isGuest) "حالت مهمان (آفلاین)" else "حساب کاربری فعال و متصل"
                        Text(
                            text = statusLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.user.isGuest) Color(0xFFD97706) else Color(0xFF059669)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = onAuthSuccess,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NooshPrimary)
                        ) {
                            Text("ورود به صفحه اصلی", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                viewModel.signOut()
                                Toast.makeText(context, "از حساب کاربری خارج شدید", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("خروج از حساب", color = Color(0xFFDC2626), fontSize = 13.sp)
                        }
                    }
                }
            }

            else -> {
                // Quick registration / sign-in Form
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp)
                    ) {
                        Text(
                            text = "مشخصات حساب",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("نام و نام خانوادگی") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NooshPrimary,
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text("آدرس ایمیل") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Email,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NooshPrimary,
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("رمز عبور (اختیاری برای Clerk)") },
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NooshPrimary,
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(22.dp))

                        var isSubmitting by remember { mutableStateOf(false) }

                        Button(
                            onClick = {
                                if (emailInput.isNotBlank()) {
                                    val name = nameInput.ifBlank { "کاربر نوش" }
                                    isSubmitting = true
                                    viewModel.signInWithEmail(emailInput.trim(), name.trim()) { message ->
                                        isSubmitting = false
                                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                        onAuthSuccess()
                                    }
                                } else {
                                    Toast.makeText(context, "لطفاً ایمیل خود را وارد کنید", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isSubmitting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NooshPrimary)
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("ورود و ساخت حساب در Clerk", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = {
                                val guestName = nameInput.trim().ifBlank { "کاربر مهمان" }
                                viewModel.continueAsGuest(guestName)
                                Toast.makeText(context, "ورود به عنوان مهمان", Toast.LENGTH_SHORT).show()
                                onAuthSuccess()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("ادامه به عنوان مهمان", fontSize = 14.sp, color = Color(0xFF475569))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}
