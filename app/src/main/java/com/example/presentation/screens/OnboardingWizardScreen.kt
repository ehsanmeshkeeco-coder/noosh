package com.example.presentation.screens

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.usecase.WaterCalculationAlgorithm
import com.example.presentation.components.NooshCharacterView
import com.example.presentation.theme.NooshPrimary
import com.example.presentation.theme.NooshSubtleBlue
import com.example.presentation.viewmodel.MainViewModel

@Composable
fun OnboardingWizardScreen(
    viewModel: MainViewModel,
    onCompleteOnboarding: () -> Unit
) {
    val context = LocalContext.current
    val dashboardState by viewModel.dashboardState.collectAsState()
    val authState by viewModel.authState.collectAsState()
    val initialProfile = dashboardState?.profile
    val authUser = (authState as? com.example.data.remote.clerk.AuthState.Authenticated)?.user

    // Step state: 0 = Profile & Photo, 1 = Biometrics (Height, Weight, Age, Gender), 2 = Calculated Goal & Result
    var currentStep by remember { mutableIntStateOf(0) }

    // Pre-populate name from auth or profile without duplicate re-entry
    val knownName = remember(initialProfile, authUser) {
        val n1 = authUser?.firstName?.takeIf { it.isNotBlank() && it != "کاربر مهمان" && it != "کاربر نوش" && it != "کاربر گرامی" }
        val n2 = initialProfile?.name?.takeIf { it.isNotBlank() && it != "کاربر مهمان" && it != "کاربر نوش" && it != "کاربر گرامی" }
        n1 ?: n2 ?: (authUser?.firstName ?: initialProfile?.name ?: "کاربر گرامی")
    }

    // Form states
    var name by remember(knownName) { mutableStateOf(knownName) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedImageBytes by remember { mutableStateOf<ByteArray?>(null) }

    var weightKg by remember(initialProfile) { mutableFloatStateOf(initialProfile?.weightKg ?: 70f) }
    var heightCm by remember(initialProfile) { mutableFloatStateOf(initialProfile?.heightCm ?: 170f) }
    var age by remember(initialProfile) { mutableIntStateOf(initialProfile?.age ?: 25) }
    var gender by remember(initialProfile) { mutableStateOf(initialProfile?.gender ?: "male") }
    var isSaving by remember { mutableStateOf(false) }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    selectedImageBytes = inputStream.readBytes()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "خطا در خواندن تصویر", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Dynamic Calculation
    val calculationResult = remember(weightKg, heightCm, age, gender) {
        WaterCalculationAlgorithm.calculateDailyGoal(
            weightKg = weightKg,
            heightCm = heightCm,
            age = age,
            gender = gender
        )
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .testTag("onboarding_wizard_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Step Indicator Dots
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(0, 1, 2).forEach { stepIndex ->
                val isActive = currentStep == stepIndex
                val isCompleted = currentStep > stepIndex

                Box(
                    modifier = Modifier
                        .size(if (isActive) 28.dp else 12.dp, 12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when {
                                isActive -> MaterialTheme.colorScheme.primary
                                isCompleted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                )
                if (stepIndex < 2) {
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedContent(
            targetState = currentStep,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "wizard_step_anim"
        ) { step ->
            when (step) {
                0 -> {
                    // Step 1: Name and Profile Photo
                    StepOneNameAndPhoto(
                        name = name,
                        onNameChange = { name = it },
                        imageUri = selectedImageUri,
                        existingImageUrl = initialProfile?.profileImageUrl,
                        onPickPhoto = { photoPickerLauncher.launch("image/*") }
                    )
                }
                1 -> {
                    // Step 2: Biometrics (Weight, Height, Age, Gender)
                    StepTwoBiometrics(
                        weightKg = weightKg,
                        onWeightChange = { weightKg = it },
                        heightCm = heightCm,
                        onHeightChange = { heightCm = it },
                        age = age,
                        onAgeChange = { age = it },
                        gender = gender,
                        onGenderChange = { gender = it }
                    )
                }
                2 -> {
                    // Step 3: Result & Personalized Recommendation
                    StepThreeWaterCalculationResult(
                        name = name,
                        calculationResult = calculationResult
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Navigation Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentStep > 0) {
                OutlinedButton(
                    onClick = { currentStep-- },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "مرحله قبل")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("مرحله قبل", fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            Button(
                onClick = {
                    if (currentStep < 2) {
                        currentStep++
                    } else {
                        // Complete onboarding: Save profile + recalculate
                        if (!isSaving) {
                            isSaving = true
                            viewModel.saveOnboardingProfile(
                                name = name,
                                weightKg = weightKg,
                                heightCm = heightCm,
                                age = age,
                                gender = gender,
                                avatarBytes = selectedImageBytes,
                                avatarUri = selectedImageUri?.toString(),
                                onComplete = {
                                    isSaving = false
                                    Toast.makeText(context, "برنامه هوشمند شما با موفقیت فعال شد!", Toast.LENGTH_SHORT).show()
                                    onCompleteOnboarding()
                                }
                            )
                        }
                    }
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = NooshPrimary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(if (currentStep > 0) 1.5f else 1f)
                    .height(52.dp)
                    .testTag("onboarding_next_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (currentStep == 2) "تأیید و شروع تور تعاملی ✨" else "مرحله بعد",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (currentStep < 2) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun StepOneNameAndPhoto(
    name: String,
    onNameChange: (String) -> Unit,
    imageUri: Uri?,
    existingImageUrl: String?,
    onPickPhoto: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NooshCharacterView(size = 72.dp)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "به «نوش» خوش آمدید! 💧",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "برای شروع بیایید یکدیگر را بهتر بشناسیم. لطفاً نام و تصویر دلخواهتان را مشخص کنید.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
            )

            // Avatar circle with photo picker
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onPickPhoto() },
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "عکس پروفایل انتخاب شده",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (!existingImageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = existingImageUrl,
                        contentDescription = "عکس پروفایل",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.AddAPhoto,
                            contentDescription = "انتخاب تصویر پروفایل",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "افزودن عکس",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("نام شما") },
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
private fun StepTwoBiometrics(
    weightKg: Float,
    onWeightChange: (Float) -> Unit,
    heightCm: Float,
    onHeightChange: (Float) -> Unit,
    age: Int,
    onAgeChange: (Int) -> Unit,
    gender: String,
    onGenderChange: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "شاخص‌های زیستی و فیزیکی",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "الگوریتم تخصصی نوش بر اساس وزن، قد، سن و جنسیت، حجم آب روزانه مورد نیاز بدنتان را محاسبه می‌کند.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            // Gender Selector
            Text(
                text = "جنسیت:",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GenderOption(
                    title = "مرد",
                    isSelected = gender == "male",
                    icon = Icons.Default.Male,
                    modifier = Modifier.weight(1f),
                    onSelect = { onGenderChange("male") }
                )
                GenderOption(
                    title = "زن",
                    isSelected = gender == "female",
                    icon = Icons.Default.Female,
                    modifier = Modifier.weight(1f),
                    onSelect = { onGenderChange("female") }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Weight Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("وزن:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = "${weightKg.toInt()} کیلوگرم",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = weightKg,
                onValueChange = { onWeightChange(it) },
                valueRange = 35f..150f,
                steps = 114,
                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Height Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("قد:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = "${heightCm.toInt()} سانتی‌متر",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = heightCm,
                onValueChange = { onHeightChange(it) },
                valueRange = 120f..220f,
                steps = 99,
                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Age Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("سن:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = "$age سال",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = age.toFloat(),
                onValueChange = { onAgeChange(it.toInt()) },
                valueRange = 12f..90f,
                steps = 77,
                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
private fun GenderOption(
    title: String,
    isSelected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onSelect() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StepThreeWaterCalculationResult(
    name: String,
    calculationResult: WaterCalculationAlgorithm.CalculationResult
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
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
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF38BDF8), MaterialTheme.colorScheme.primary)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WaterDrop,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "برنامه هیدراتاسیون هوشمند شما",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = calculationResult.explanation,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
            )

            // Result Metric Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricBox(
                    title = "هدف روزانه",
                    value = "${calculationResult.dailyWaterGoalMl} ml",
                    subtitle = "${calculationResult.recommendedGlasses} لیوان ۲۵۰ میلی‌لیتری",
                    modifier = Modifier.weight(1f)
                )
                MetricBox(
                    title = "فاصله یادآورها",
                    value = "هر ${calculationResult.recommendedIntervalMinutes} دقیقه",
                    subtitle = "در بازه بیداری",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "تمامی یادآورها و اهداف به طور خودکار تنظیم و ذخیره خواهند شد.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricBox(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), textAlign = TextAlign.Center)
        }
    }
}
