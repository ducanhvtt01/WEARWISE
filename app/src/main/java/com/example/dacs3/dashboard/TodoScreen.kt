package com.example.dacs3.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dacs3.connectDB.DashboardViewModel
import com.example.dacs3.connectDB.TodoDbModel
import com.example.dacs3.connectDB.ClothingItem
import com.example.dacs3.connectDB.supabase
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.BottomSheetDefaults

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TodoScreen(
    viewModel: DashboardViewModel,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userId = supabase.auth.currentUserOrNull()?.id ?: ""

    // Tải danh sách công việc khi mở màn hình
    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            viewModel.fetchTodos(userId)
            viewModel.getClothingItems(userId)
        }
    }

    val todosList by viewModel.todos.collectAsState()
    val clothesList by viewModel.clothingItems.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Chuyến đi, 1: Mua sắm (Wishlist), 2: Chăm sóc & Thanh lý
    val tabs = listOf("Event Packing", "Wishlist", "Care & Declutter")

    // Trạng thái hộp thoại thêm tác vụ thủ công
    var showAddDialog by remember { mutableStateOf(false) }
    var newTodoTitle by remember { mutableStateOf("") }
    var newTodoDesc by remember { mutableStateOf("") }
    var newTodoType by remember { mutableStateOf("event_packing") }
    var newTodoDueDate by remember { mutableStateOf("") }

    // Trạng thái hộp thoại gợi ý từ AI
    var showAiSuggestionDialog by remember { mutableStateOf(false) }
    var suggestionEventName by remember { mutableStateOf("") }
    var suggestionDays by remember { mutableStateOf("3") }
    var suggestionWeather by remember { mutableStateOf("Warm") }
    var isGeneratingSuggestions by remember { mutableStateOf(false) }
    var generatedSuggestions by remember { mutableStateOf<List<Pair<String, Boolean>>>(emptyList()) }

    // Trạng thái hộp thoại thêm vào tủ đồ khi hoàn thành Wishlist
    var showAddToClosetDialog by remember { mutableStateOf(false) }
    var completedWishlistTodoName by remember { mutableStateOf("") }
    var newClothName by remember { mutableStateOf("") }
    var newClothCategory by remember { mutableStateOf("Top") }
    var newClothColor by remember { mutableStateOf("Black") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            // Tiêu đề
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .padding(top = 40.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Wardrobe To-Do List",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }

            // Thanh chọn Tab phong cách Glassmorphism
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(4.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    val tabBg by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        label = "tabBg_$index"
                    )
                    val tabTextColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        label = "tabTextColor_$index"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(tabBg)
                            .clickable { selectedTab = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = tabTextColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Lọc danh sách theo Tab
            val filteredTodos = when (selectedTab) {
                0 -> todosList.filter { it.type == "event_packing" }
                1 -> todosList.filter { it.type == "wishlist" }
                else -> todosList.filter { it.type == "declutter" || it.type == "laundry" || it.type == "weather" }
            }

            // Banner gợi ý từ AI cho Chuyến đi & Mua sắm
            if (selectedTab == 0 && filteredTodos.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = "AI",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Planning a trip or event?",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Let AI suggest a complete packing checklist for your next destination!",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { showAiSuggestionDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Ask AI Suggestions", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            if (filteredTodos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.PlaylistAddCheck,
                            contentDescription = "No tasks",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No tasks found here",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tap the '+' button below to add a new task.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(filteredTodos, key = { it.id ?: "" }) { todo ->
                        TodoCard(
                            todo = todo,
                            onCheckChanged = { isChecked ->
                                if (todo.id != null) {
                                    viewModel.updateTodoStatus(todo.id, isChecked, userId)
                                    // Nếu hoàn thành mục wishlist, gợi ý thêm đồ mới vào tủ
                                    if (todo.type == "wishlist" && isChecked) {
                                        completedWishlistTodoName = todo.title.replace("Buy a new ", "").replace("Buy ", "")
                                        newClothName = completedWishlistTodoName
                                        showAddToClosetDialog = true
                                    }
                                }
                            },
                            onDelete = {
                                if (todo.id != null) {
                                    viewModel.deleteTodo(todo.id, userId)
                                }
                            }
                        )
                    }
                }
            }
        }

        // Nút nổi (FAB) để thêm tác vụ thủ công
        FloatingActionButton(
            onClick = {
                // Đặt loại mặc định dựa theo tab đang chọn
                newTodoType = when (selectedTab) {
                    0 -> "event_packing"
                    1 -> "wishlist"
                    else -> "declutter"
                }
                newTodoTitle = ""
                newTodoDesc = ""
                newTodoDueDate = ""
                showAddDialog = true
            },
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 24.dp, end = 24.dp)
                .shadow(8.dp, CircleShape)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add Task", tint = MaterialTheme.colorScheme.onPrimary)
        }
    }

    // Hộp thoại thêm tác vụ thủ công
    if (showAddDialog) {

        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { showAddDialog = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Tiêu đề
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 24.dp)) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.TaskAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Add New Task ✨", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    TextField(
                        value = newTodoTitle,
                        onValueChange = { newTodoTitle = it },
                        placeholder = { Text("Task Title", fontSize = 15.sp) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                    TextField(
                        value = newTodoDesc,
                        onValueChange = { newTodoDesc = it },
                        placeholder = { Text("Description (Optional)", fontSize = 15.sp) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                    
                Text("TASK TYPE", fontWeight = FontWeight.Black, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 24.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val types = listOf("event_packing" to "Event Packing", "wishlist" to "Wishlist", "declutter" to "Care & Declutter")
                    items(types) { (id, label) ->
                        val isSelected = newTodoType == id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable { newTodoType = id }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                label,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Text("DUE DATE", fontWeight = FontWeight.Black, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).clip(RoundedCornerShape(16.dp)).clickable {
                        val calendar = Calendar.getInstance()
                        android.app.DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val selectedCal = Calendar.getInstance()
                                selectedCal.set(year, month, dayOfMonth)
                                val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                newTodoDueDate = format.format(selectedCal.time)
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (newTodoDueDate.isNotBlank()) newTodoDueDate else "Select Due Date",
                            color = if (newTodoDueDate.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(Icons.Default.CalendarToday, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                }

                // Nút hành động
                Button(
                    onClick = {
                        if (newTodoTitle.isNotBlank()) {
                            val todo = TodoDbModel(
                                userId = userId,
                                title = newTodoTitle,
                                description = if (newTodoDesc.isNotBlank()) newTodoDesc else null,
                                type = newTodoType,
                                dueDate = if (newTodoDueDate.isNotBlank()) newTodoDueDate else null,
                                isCompleted = false
                            )
                            viewModel.saveTodo(todo) { success ->
                                if (success) showAddDialog = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Save Task", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Hộp thoại gợi ý bằng AI
    if (showAiSuggestionDialog) {
        Dialog(onDismissRequest = { if (!isGeneratingSuggestions) showAiSuggestionDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("AI Packing Helper", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (generatedSuggestions.isEmpty() && !isGeneratingSuggestions) {
                            Text(
                                "AI will suggest a personalized travel checklist based on the destination and weather.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = suggestionEventName,
                                onValueChange = { suggestionEventName = it },
                                label = { Text("Destination / Event Name") },
                                placeholder = { Text("e.g., Dalat Vacation") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = suggestionDays,
                                onValueChange = { suggestionDays = it },
                                label = { Text("Number of Days") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = suggestionWeather,
                                onValueChange = { suggestionWeather = it },
                                label = { Text("Expected Weather") },
                                placeholder = { Text("e.g., Rainy, Cold (15°C)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        } else if (isGeneratingSuggestions) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(48.dp), color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(24.dp))
                                Text("AI is curating suggestions...", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            }
                        } else {
                            Text("Select items to add to your To-Do list:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            generatedSuggestions.forEachIndexed { idx, (itemText, checked) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (checked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent)
                                        .clickable {
                                            generatedSuggestions = generatedSuggestions.toMutableList().also {
                                                it[idx] = itemText to !checked
                                            }
                                        }
                                        .padding(vertical = 8.dp, horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = { isChecked ->
                                            generatedSuggestions = generatedSuggestions.toMutableList().also {
                                                it[idx] = itemText to (isChecked == true)
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(itemText, fontWeight = if (checked) FontWeight.Medium else FontWeight.Normal)
                                }
                            }
                        }
                    }

                    // Nút hành động
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isGeneratingSuggestions) {
                            TextButton(
                                onClick = {
                                    showAiSuggestionDialog = false
                                    generatedSuggestions = emptyList()
                                }
                            ) {
                                Text("Cancel", fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        if (generatedSuggestions.isEmpty() && !isGeneratingSuggestions) {
                            Button(
                                onClick = {
                                    if (suggestionEventName.isNotBlank()) {
                                        isGeneratingSuggestions = true
                                        val daysInt = suggestionDays.toIntOrNull() ?: 3
                                        viewModel.generatePackingSuggestions(
                                            destination = suggestionEventName,
                                            tripDays = daysInt,
                                            weatherTemp = suggestionWeather
                                        ) { items ->
                                            generatedSuggestions = items.map { it to true }
                                            isGeneratingSuggestions = false
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Ask AI", fontWeight = FontWeight.Bold)
                            }
                        } else if (generatedSuggestions.isNotEmpty()) {
                            Button(
                                onClick = {
                                    val selectedItems = generatedSuggestions.filter { it.second }.map { it.first }
                                    if (selectedItems.isNotEmpty()) {
                                        scope.launch {
                                            selectedItems.forEach { item ->
                                                val todo = TodoDbModel(
                                                    userId = userId,
                                                    title = "Pack: $item",
                                                    description = "AI recommended for trip to $suggestionEventName",
                                                    type = "event_packing",
                                                    isCompleted = false
                                                )
                                                viewModel.saveTodo(todo)
                                            }
                                            showAiSuggestionDialog = false
                                            generatedSuggestions = emptyList()
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Add to List", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Hộp thoại thêm món đồ đã mua trong Wishlist vào Tủ đồ
    if (showAddToClosetDialog) {
        Dialog(onDismissRequest = { showAddToClosetDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Checkroom, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Add to Closet", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            "Congratulations on your purchase! Let's add it to your closet.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = newClothName,
                            onValueChange = { newClothName = it },
                            label = { Text("Item Name") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        Text("Category", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val categories = listOf("Top", "Bottom", "Outerwear", "Shoes", "Accessories")
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(categories) { cat ->
                                val isSelected = newClothCategory == cat
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { newClothCategory = cat },
                                    label = { Text(cat) },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                        
                        OutlinedTextField(
                            value = newClothColor,
                            onValueChange = { newClothColor = it },
                            label = { Text("Color") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Nút hành động
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showAddToClosetDialog = false }) {
                            Text("Skip", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newClothName.isNotBlank()) {
                                    val newItem = ClothingItem(
                                        userId = userId,
                                        clothes_name = newClothName,
                                        category = newClothCategory,
                                        mainColor = newClothColor,
                                        seasons = listOf("All"),
                                        occasions = listOf("Casual"),
                                        status = "AVAILABLE"
                                    )
                                    viewModel.addClothing(newItem) {
                                        showAddToClosetDialog = false
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Add to Closet", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TodoCard(
    todo: TodoDbModel,
    onCheckChanged: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (todo.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (todo.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left color indicator strip based on completion status
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(
                        if (todo.isCompleted) Color(0xFF4CAF50)
                        else MaterialTheme.colorScheme.primary
                    )
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = todo.isCompleted,
                    onCheckedChange = { onCheckChanged(it == true) }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = todo.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (todo.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary,
                        textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!todo.description.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = todo.description,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (!todo.dueDate.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Due: ${todo.dueDate}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
