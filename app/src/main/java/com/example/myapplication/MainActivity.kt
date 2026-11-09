package com.example.myapplication12

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ================================================================
// ГЛОБАЛЬНЫЕ НАСТРОЙКИ И РОЛИ
// ================================================================
data class AppSettings(val isDarkTheme: Boolean = false, val showHints: Boolean = true)
val LocalSettings = androidx.compose.runtime.compositionLocalOf { AppSettings() }

enum class Role { EMPLOYEE, ADMIN }
// Глобальный список напоминаний
// Глобальный список напоминаний (теперь наблюдаемый!)
// Модель напоминания (добавили isDone)
data class ReminderTask(val id: Int, val title: String, val isDone: Boolean = false)

object RemindersRepo {
    var nextId = 3
    val items = mutableStateListOf(
        ReminderTask(1, "Купить продукты"),
        ReminderTask(2, "Позвонить маме")
    )

    fun add(title: String) { items.add(ReminderTask(nextId++, title)) }

    // Считаем только НЕвыполненные задачи
    fun count() = items.count { !it.isDone }
}
// ================================================================
// MAIN ACTIVITY
// ================================================================
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Защита от несанкционированного внешнего запуска (FIND-006)
        if (intent.action != android.content.Intent.ACTION_MAIN) {
            finish()
            return
        }
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                MainApp()
            }
        }
    }
}

// ================================================================
// ГЛАВНОЕ ПРИЛОЖЕНИЕ
// ================================================================
@Composable
fun MainApp() {
    var currentUserRole by remember { mutableStateOf<Role?>(null) }
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var settings by remember { mutableStateOf(AppSettings()) }

    // Экран входа (ЛР13)
    if (currentUserRole == null) {
        Lab13AuthScreen(onLoginSuccess = { role -> currentUserRole = role })
        return
    }

    // Список вкладок (базовые для всех)
    val tabs = mutableListOf(
        TabItem("Профиль", Icons.Default.Person) { UserProfileScreen() },
        TabItem("Напоминания", Icons.Default.DateRange) { RemindersScreen() },
        TabItem("Посты", Icons.Default.Notifications) { PostsScreen() },
        TabItem("Настройки", Icons.Default.Settings) {
            SettingsScreen(
                isDarkTheme = settings.isDarkTheme,
                onThemeToggle = { settings = settings.copy(isDarkTheme = it) },
                currentUserRole = currentUserRole
            )
        }
    )

    // Кнопка Выхода (есть у всех)
    tabs.add(TabItem("Выход", Icons.Default.ArrowBack) {
        LaunchedEffect(Unit) { currentUserRole = null }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Выход...")
        }
    })

    // Отрисовка интерфейса
    CompositionLocalProvider(LocalSettings provides settings) {
        MaterialTheme(
            colorScheme = if (settings.isDarkTheme) darkColorScheme() else lightColorScheme()
        ) {
            Scaffold(
                bottomBar = {
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        edgePadding = 8.dp
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { Text(tab.label) },
                                icon = { Icon(tab.icon, contentDescription = tab.label) }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    tabs[selectedTab].content()
                }
            }
        }
    }
}
data class TabItem(val label: String, val icon: ImageVector, val content: @Composable () -> Unit)

// ================================================================
// ЭКРАН ВХОДА (ЛР13)
// ================================================================

private const val SECURE_PASSWORD = "1234"
@Composable
fun Lab13AuthScreen(onLoginSuccess: (Role) -> Unit) {
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("Вход в систему", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(value = login, onValueChange = { login = it; errorMsg = "" }, label = { Text("Логин") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = password, onValueChange = { password = it; errorMsg = "" }, label = { Text("Пароль") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())

        if (errorMsg.isNotEmpty()) {
            Text(errorMsg, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                if (login == "admin" && password == SECURE_PASSWORD) onLoginSuccess(Role.ADMIN)
                else if (login == "user" && password == SECURE_PASSWORD) onLoginSuccess(Role.EMPLOYEE)
                else errorMsg = "Неверный логин или пароль"
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Войти") }
    }
}

// ================================================================
// ПРОФИЛЬ (С КНОПКАМИ И ГРАДИЕНТОМ)
// ================================================================
@Composable
fun UserProfileScreen() {
    var currentName by remember { mutableStateOf("Анна Расписаниева") }
    var nameInput by remember { mutableStateOf(currentName) }
    var isEditing by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showSavedMessage by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        // Градиентная шапка
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))),
            contentAlignment = Alignment.Center
        ) {
            Text("Мой профиль", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }

            Spacer(Modifier.height(8.dp))

            // Имя с кнопкой редактирования (карандашик)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(currentName, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { isEditing = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Редактировать", modifier = Modifier.size(20.dp))
                }
            }

            Text("Планирую день продуктивно", color = Color.Gray, fontSize = 14.sp)

            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("128", fontWeight = FontWeight.Bold); Text("Подписчики", color = Color.Gray) }
                Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("64", fontWeight = FontWeight.Bold); Text("Подписки", color = Color.Gray) }
            }

            Spacer(Modifier.height(24.dp))

            if (isEditing) {
                OutlinedTextField(value = nameInput, onValueChange = { nameInput = it }, label = { Text("Новое имя") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
            }

            // Кнопки
            Button(onClick = { currentName = nameInput; isEditing = false }, modifier = Modifier.fillMaxWidth()) { Text("Сохранить") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { isEditing = false }, modifier = Modifier.fillMaxWidth()) { Text("Отмена") }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { nameInput = "Анна Расписаниева" }, modifier = Modifier.fillMaxWidth()) { Text("Сброс") }

            Spacer(Modifier.height(16.dp))

            // Кнопка с загрузкой
            Button(
                onClick = {
                    if (nameInput.isNotBlank()) {
                        isLoading = true; showSavedMessage = false
                        scope.launch { delay(1500); isLoading = false; showSavedMessage = true }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                else Text("Сохранить изменения")
            }

            if (showSavedMessage) {
                Spacer(Modifier.height(8.dp))
                Text("Изменения сохранены", color = Color.Green, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ================================================================
// СЧЁТЧИК И ФОРМА
// ================================================================

// ================================================================
// ПОСТЫ (ЛР10)
// ================================================================
@Composable
fun PostsScreen() {
    var posts by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(1000)
        posts = (1..10).map { "Новость №$it: Как выбрать подарок" }
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Каталог статей", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        if (isLoading) CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        else LazyColumn { items(posts) { post -> Card(Modifier.fillMaxWidth().padding(4.dp)) { Text(post, Modifier.padding(16.dp)) } } }
    }
}

// ================================================================
// ПРОПУСК И ЖУРНАЛ (ЛР14)
// ================================================================
@Composable
fun Lab14AccessScreen(role: Role) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(80.dp))
        Spacer(Modifier.height(16.dp))
        Text("Мобильный пропуск", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Ваша роль: $role", color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(32.dp))
        Card(Modifier.size(200.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("QR-код", fontSize = 32.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun AdminJournalScreen() {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Журнал сканирований", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Card(Modifier.fillMaxWidth().padding(4.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Охранник отсканировал пропуск", fontWeight = FontWeight.Bold)
                Text("Результат: ALLOWED", color = Color.Green)
            }
        }
    }
}
// ================================================================
// СПИСОК НАПОМИНАНИЙ
// ================================================================

        @Composable
        fun SettingsScreen(
            isDarkTheme: Boolean,
            onThemeToggle: (Boolean) -> Unit,
            onBack: () -> Unit
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text("Настройки", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Тёмная тема", fontSize = 18.sp)
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = onThemeToggle
                    )
                }
            }
        }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isDarkTheme: Boolean,
    onThemeToggle: (Boolean) -> Unit,
    currentUserRole: Role?
) {
    // Состояние: какую настройку открыли (null = главное меню настроек)
    var currentSettingScreen by remember { mutableStateOf<String?>(null) }

    if (currentSettingScreen == "PASS") {
        // Экран Пропуска (ЛР14) с кнопкой Назад
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Мобильный пропуск") },
                navigationIcon = {
                    IconButton(onClick = { currentSettingScreen = null }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
            Lab14AccessScreen(currentUserRole!!)
        }
    } else if (currentSettingScreen == "AUDIT") {
        // Экран Журнала с кнопкой Назад
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Журнал сканирований") },
                navigationIcon = {
                    IconButton(onClick = { currentSettingScreen = null }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
            AdminJournalScreen()
        }
    } else {
        // Главное меню настроек
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Настройки", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))

            // Тёмная тема
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Тёмная тема", fontSize = 18.sp)
                Switch(checked = isDarkTheme, onCheckedChange = onThemeToggle)
            }

            Spacer(Modifier.height(32.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            // Админские настройки (видны только Админу)
            if (currentUserRole == Role.ADMIN) {
                Text("Панель администратора", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = { currentSettingScreen = "PASS" },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Мой пропуск (QR)")
                }

                OutlinedButton(
                    onClick = { currentSettingScreen = "AUDIT" },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Icon(Icons.Default.List, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Журнал сканирований")
                }
            }
        }
    }
}
// ================================================================
// НАПОМИНАНИЯ (Счётчик + Список + Форма)
// ================================================================
@Composable
fun RemindersScreen() {
    var input by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // 1. СЧЁТЧИК
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Активных напоминаний", fontSize = 16.sp, color = Color.Gray)
                Text(
                    text = "${RemindersRepo.count()}",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // 2. ФОРМА ДОБАВЛЕНИЯ
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("Новое напоминание") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    if (input.isNotBlank()) {
                        RemindersRepo.add(input)
                        input = ""
                    }
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить")
            }
        }

        Spacer(Modifier.height(16.dp))

        // 3. СПИСОК С ГАЛОЧКАМИ
        Text("Мои задачи", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        val items = RemindersRepo.items

        if (items.isEmpty()) {
            Text("Список пуст", color = Color.Gray)
        } else {
            LazyColumn {
                items(items) { task: ReminderTask ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = task.isDone,
                                onCheckedChange = { isChecked ->
                                    val index = items.indexOf(task)
                                    if (index != -1) {
                                        items[index] = task.copy(isDone = isChecked)
                                    }
                                }
                            )

                            Text(
                                text = task.title,
                                fontSize = 16.sp,
                                textDecoration = if (task.isDone) {
                                    androidx.compose.ui.text.style.TextDecoration.LineThrough
                                } else {
                                    null
                                },
                                color = if (task.isDone) Color.Gray else Color.Unspecified,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }

    }
}

@Composable
fun VulnerableWebView() {
    android.webkit.WebView(androidx.compose.ui.platform.LocalContext.current).apply {
        settings.javaScriptEnabled = false
        settings.allowFileAccess = true
        loadUrl("https://example.com")
    }
}