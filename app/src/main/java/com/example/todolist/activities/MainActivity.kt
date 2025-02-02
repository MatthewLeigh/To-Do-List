package com.example.todolist.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.todolist.dialogs.BottomSheetDialog
import com.example.todolist.R
import com.example.todolist.task.TaskTable
import com.example.todolist.task.TaskAdapter
import com.example.todolist.task.TaskViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar

class MainActivity :
    AppCompatActivity(),
    TaskAdapter.TaskClickDeleteInterface,
    TaskAdapter.TaskClickManageInterface,
    TaskAdapter.TaskClickToggleIsCheckedInterface
{

    // UI Components
    private lateinit var taskList: RecyclerView
    private lateinit var addButton: FloatingActionButton
    private lateinit var mainBottomNav: BottomNavigationView
    private lateinit var toolbar: MaterialToolbar
    private lateinit var toggleCountdownSwitch: SwitchCompat

    // ViewModel
    private lateinit var taskViewModel: TaskViewModel

    // Current filter state
    private var currentFilter: String = "none"

    // Toggle Due Date / Countdown State
    private var showCountdown: Boolean = false

    // Initialize UI components
    private fun setupUI() {
        taskList = findViewById(R.id.MainTaskListRV)
        addButton = findViewById(R.id.MainAddButton)
        mainBottomNav = findViewById(R.id.MainBottomNav)
        toolbar = findViewById(R.id.toolbar)
        toggleCountdownSwitch = findViewById(R.id.toggleCountdownSwitch)
    }

    // Set up ViewModel
    private fun setupViewModel() {
        taskViewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[TaskViewModel::class.java]

        // Observe all tasks from ViewModel
        taskViewModel.allTasks.observe(this) { tasks ->
            tasks?.let {
                Log.d("MainActivity", "Observed tasks: ${tasks.size} items")
                applyFilter(tasks, currentFilter, taskList.adapter as TaskAdapter)
            }
        }
    }

    // Set up RecyclerView
    private fun setupRecyclerView() {
        val taskAdapterMain = TaskAdapter(this, this, this)
        taskList.adapter = taskAdapterMain
        taskList.layoutManager = LinearLayoutManager(this)
    }

    // Set up toolbar
    private fun setupToolbar() {
        toolbar.title = "To-Do List"
        toolbar.subtitle = "All Tasks"

        // Set up the toggle switch listener
        toggleCountdownSwitch.setOnCheckedChangeListener { _, isChecked ->

            showCountdown = isChecked
            toggleCountdownSwitch.text = if (isChecked) {
                "Countdown"
            } else {
                "Due Date"
            }
            Log.d("MainActivity", "Toggle switch changed. Show countdown: $showCountdown")
            (taskList.adapter as TaskAdapter).updateShowCountdown(showCountdown)
        }
    }

    // Set up FAB click listener
    private fun setupAddButton() {
        addButton.setOnClickListener {
            Log.d("MainActivity", "Add button clicked")
            val intent = Intent(this@MainActivity, ManageTaskActivity::class.java)
            startActivity(intent)
        }
    }

    // Set up bottom navigation listener
    private fun setupBottomNavigation() {
        mainBottomNav.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_all_tasks -> {
                    currentFilter = "none"
                    Log.d("MainActivity", "Filter: Show all tasks")
                    applyFilter(taskViewModel.allTasks.value ?: emptyList(), currentFilter, taskList.adapter as TaskAdapter)
                    toolbar.subtitle = "All Tasks"
                    true
                }
                R.id.nav_completed_tasks -> {
                    currentFilter = "isComplete"
                    Log.d("MainActivity", "Filter: Show completed tasks")
                    applyFilter(taskViewModel.allTasks.value ?: emptyList(), currentFilter, taskList.adapter as TaskAdapter)
                    toolbar.subtitle = "Complete Tasks"
                    true
                }
                R.id.nav_outsanding_tasks -> {
                    currentFilter = "outstanding"
                    Log.d("MainActivity", "Filter: Show outstanding tasks")
                    applyFilter(taskViewModel.allTasks.value ?: emptyList(), currentFilter, taskList.adapter as TaskAdapter)
                    toolbar.subtitle = "Outstanding Tasks"
                    true
                }
                R.id.nav_overdue_tasks -> {
                    currentFilter = "overdue"
                    Log.d("MainActivity", "Filter: Show overdue tasks")
                    applyFilter(taskViewModel.allTasks.value ?: emptyList(), currentFilter, taskList.adapter as TaskAdapter)
                    toolbar.subtitle = "Overdue Tasks"
                    true
                }
                else -> false
            }
        }
    }

    // Apply filter to the task list
    private fun applyFilter(list: List<TaskTable>, filter: String, taskAdapterMain: TaskAdapter) {
        val filteredList = when (filter) {
            "none" -> list
            "isComplete" -> list.filter { it.isComplete }
            "outstanding" -> list.filter { !it.isComplete }
            "overdue" -> list.filter { task ->
                task.taskDueDateTime.isBefore(
                    java.time.Instant.ofEpochSecond(System.currentTimeMillis() / 1000)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDateTime()
                ) && !task.isComplete
            }
            else -> list
        }
        Log.d("MainActivity", "Applied filter: $filter. Filtered list size: ${filteredList.size}")
        taskAdapterMain.updateList(filteredList)
    }

    // Helper function to show Snackbar
    private fun showSnackbar(message: String) {

        val snackbar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
        val snackbarView = snackbar.view
        val layoutParams = snackbarView.layoutParams as ViewGroup.MarginLayoutParams

        snackbar.setAnchorView(mainBottomNav)
        layoutParams.marginStart = 24
        layoutParams.marginEnd = 280
        layoutParams.bottomMargin = 60
        snackbarView.layoutParams = layoutParams

        snackbarView.setOnClickListener {
            snackbar.dismiss()
        }

        snackbar.show()
    }

    // Check for return message from ManageTaskActivity
    private fun checkForSnackbarMessageInIntent() {
        val snackbarMessage = intent.getStringExtra("snackbar_message")
        if (!snackbarMessage.isNullOrEmpty()) {
            showSnackbar(snackbarMessage)
        }
    }

    // Handle create activity
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        super.onCreate(savedInstanceState)

        setupUI()
        setupViewModel()
        setupRecyclerView()
        setupToolbar()
        setupAddButton()
        setupBottomNavigation()
        checkForSnackbarMessageInIntent()
        Log.d("MainActivity", "Activity created and initialized")
    }

    // Handle task deletion
    override fun onDeleteButtonClick(taskTable: TaskTable) {
        Log.d("MainActivity", "Delete button clicked for task: ${taskTable.taskTitle}")
        taskViewModel.deleteTask(taskTable)
        showSnackbar("Task Deleted")
    }

    // Handle task item click (show bottom sheet)
    override fun onTaskItemClick(taskTable: TaskTable) {
        Log.d("MainActivity", "Task item clicked: ${taskTable.taskTitle}")
        val bottomSheetDialog = BottomSheetDialog.newInstance(taskTable)
        bottomSheetDialog.show(supportFragmentManager, "TaskBottomSheet")
    }

    // Handle task checkbox toggle
    override fun onTaskCheckBoxToggled(taskTable: TaskTable) {
        val isCompleteNew = !taskTable.isComplete
        Log.d("MainActivity", "Checkbox toggled for task: ${taskTable.taskTitle}. New status: $isCompleteNew")
        taskViewModel.updateTaskCompletionStatus(taskTable.taskId, isCompleteNew)
        showSnackbar("Task marked as ${if (isCompleteNew) "completed" else "incomplete"}")
    }
}