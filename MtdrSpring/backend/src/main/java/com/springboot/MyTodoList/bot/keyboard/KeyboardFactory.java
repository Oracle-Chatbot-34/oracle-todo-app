package com.springboot.MyTodoList.bot.keyboard;

import com.springboot.MyTodoList.model.ToDoItem;
import com.springboot.MyTodoList.model.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating different types of Telegram bot keyboards
 */
public class KeyboardFactory {

    /**
     * Enhanced keyboard for main menu with better layout and organization
     */
    public static ReplyKeyboardMarkup createMainMenuKeyboard(User user) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true); // More compact keys
        keyboardMarkup.setSelective(true); // Only certain users see it
        List<KeyboardRow> keyboard = new ArrayList<>();

        // First row - Task Management
        KeyboardRow taskRow = new KeyboardRow();
        taskRow.add("📝 My Tasks");
        taskRow.add("📝 Create Task");
        keyboard.add(taskRow);

        // Second row - Sprint Management (for developers and managers)
        if (user.isDeveloper() || user.isManager()) {
            KeyboardRow sprintRow = new KeyboardRow();
            sprintRow.add("🏃‍♂️ Sprint Management");
            sprintRow.add("🔄 My Active Tasks");
            keyboard.add(sprintRow);
        }

        // Third row - Management options (for managers)
        if (user.isManager()) {
            KeyboardRow managerRow = new KeyboardRow();
            managerRow.add("👥 Team Management");
            managerRow.add("📊 KPI Dashboard");
            keyboard.add(managerRow);
        } else if (user.isDeveloper()) {
            // Developers get access to KPI dashboard
            KeyboardRow developerRow = new KeyboardRow();
            developerRow.add("📊 KPI Dashboard");
            keyboard.add(developerRow);
        }

        // Last row - Help and hide
        KeyboardRow helpRow = new KeyboardRow();
        helpRow.add("❓ Help");
        helpRow.add("❌ Hide Keyboard");
        keyboard.add(helpRow);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    /**
     * Create keyboard for task creation confirmation
     */
    public static ReplyKeyboardMarkup createTaskConfirmationKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add("Yes, create task");
        row.add("No, cancel");
        keyboard.add(row);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    /**
     * Create keyboard for task priority selection
     */
    public static ReplyKeyboardMarkup createTaskPriorityKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add("High");
        row.add("Medium");
        row.add("Low");
        keyboard.add(row);

        row = new KeyboardRow();
        row.add("Cancel");
        keyboard.add(row);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    /**
     * Create keyboard for team member selection
     */
    public static ReplyKeyboardMarkup createTeamMemberSelectionKeyboard(List<User> teamMembers, User currentUser) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> keyboard = new ArrayList<>();

        // Add self-assignment option
        KeyboardRow selfRow = new KeyboardRow();
        selfRow.add("Me (" + currentUser.getFullName() + ")");
        keyboard.add(selfRow);

        // Add other team members
        for (User member : teamMembers) {
            if (!member.getId().equals(currentUser.getId())) {
                KeyboardRow row = new KeyboardRow();
                row.add(member.getFullName() + " (ID: " + member.getId() + ")");
                keyboard.add(row);
            }
        }

        // Add cancel option
        KeyboardRow cancelRow = new KeyboardRow();
        cancelRow.add("Cancel");
        keyboard.add(cancelRow);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    /**
     * Create keyboard for task list
     */
    public static ReplyKeyboardMarkup createTaskListKeyboard(List<ToDoItem> tasks) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> keyboard = new ArrayList<>();

        // Command back to main screen
        KeyboardRow mainScreenRow = new KeyboardRow();
        mainScreenRow.add("🏠 Main Menu");
        keyboard.add(mainScreenRow);

        KeyboardRow addRow = new KeyboardRow();
        addRow.add("📝 Create New Task");
        addRow.add("🔄 My Active Tasks");
        keyboard.add(addRow);

        KeyboardRow sprintRow = new KeyboardRow();
        sprintRow.add("🏃‍♂️ Sprint Management");
        sprintRow.add("✅ Mark Task Complete");
        keyboard.add(sprintRow);

        // Task action rows - only show if there are tasks
        if (!tasks.isEmpty()) {
            // First, show active tasks for quick completion
            List<ToDoItem> activeTasks = new ArrayList<>();
            for (ToDoItem task : tasks) {
                if (!task.isDone()) {
                    activeTasks.add(task);
                    if (activeTasks.size() >= 3) { // Limit to 3 tasks for keyboard
                        break;
                    }
                }
            }

            // Add task completion shortcuts for active tasks
            for (ToDoItem task : activeTasks) {
                KeyboardRow row = new KeyboardRow();
                String taskText = task.getID() + "-DONE: " + task.getTitle();
                if (taskText.length() > 30) {
                    taskText = taskText.substring(0, 27) + "...";
                }
                row.add(taskText);
                keyboard.add(row);
            }
        }

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    /**
     * Enhanced keyboard for sprint management
     */
    public static ReplyKeyboardMarkup createSprintManagementKeyboard(boolean hasActiveSprint, boolean isManager) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> keyboard = new ArrayList<>();

        // First row - Basic sprint views
        KeyboardRow row1 = new KeyboardRow();
        row1.add("📊 Sprint Board");
        row1.add("📋 My Tasks");
        keyboard.add(row1);

        // Second row - Task management
        KeyboardRow row2 = new KeyboardRow();
        row2.add("➕ Add Task to Sprint");
        row2.add("✅ Complete Task");
        keyboard.add(row2);

        // Third row - Manager options
        if (isManager) {
            KeyboardRow row3 = new KeyboardRow();

            if (hasActiveSprint) {
                row3.add("⏹️ End Active Sprint");
            } else {
                row3.add("🆕 Create New Sprint");
            }

            row3.add("📜 Sprint History");
            keyboard.add(row3);
        }

        // Last row - Return to main menu
        KeyboardRow lastRow = new KeyboardRow();
        lastRow.add("🏠 Main Menu");
        keyboard.add(lastRow);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    /**
     * Create keyboard for sprint creation confirmation
     */
    public static ReplyKeyboardMarkup createSprintConfirmationKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add("Yes, create sprint");
        row.add("No, cancel");
        keyboard.add(row);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    /**
     * Create keyboard for selecting tasks to add to sprint
     */
    public static ReplyKeyboardMarkup createTaskSelectionForSprintKeyboard(List<ToDoItem> tasks) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> keyboard = new ArrayList<>();

        for (ToDoItem task : tasks) {
            KeyboardRow row = new KeyboardRow();
            String taskText = "ID: " + task.getID() + " - " + task.getTitle();
            if (taskText.length() > 30) {
                taskText = taskText.substring(0, 27) + "...";
            }
            row.add(taskText);
            keyboard.add(row);
        }

        KeyboardRow row = new KeyboardRow();
        row.add("Cancel");
        keyboard.add(row);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    /**
     * Create keyboard for sprint board display
     */
    public static ReplyKeyboardMarkup createSprintBoardKeyboard(boolean isManager) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add("🔄 My Active Tasks");
        row.add("📝 Create New Task");
        keyboard.add(row);

        row = new KeyboardRow();
        row.add("✅ Mark Task Complete");
        row.add("➕ Assign Task to Sprint");
        keyboard.add(row);

        if (isManager) {
            row = new KeyboardRow();
            row.add("📅 Sprint Management");
            row.add("👥 Team Management");
            keyboard.add(row);
        }

        row = new KeyboardRow();
        row.add("🏠 Main Menu");
        keyboard.add(row);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    /**
     * Enhanced keyboard after task completion
     */
    public static ReplyKeyboardMarkup createAfterTaskCompletionKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> keyboard = new ArrayList<>();

        // First row - Next logical actions
        KeyboardRow row1 = new KeyboardRow();
        row1.add("🔄 My Active Tasks");
        row1.add("📝 Create New Task");
        keyboard.add(row1);

        // Second row - Sprint options
        KeyboardRow row2 = new KeyboardRow();
        row2.add("🏃‍♂️ Sprint Management");
        row2.add("📊 Sprint Board");
        keyboard.add(row2);

        // Third row - Return to main menu
        KeyboardRow row3 = new KeyboardRow();
        row3.add("🏠 Main Menu");
        keyboard.add(row3);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    /**
     * Create empty keyboard (remove keyboard)
     */
    public static ReplyKeyboardRemove createEmptyKeyboard() {
        return new ReplyKeyboardRemove(true);
    }

    /**
     * Create keyboard for developer task menu
     */
    public static ReplyKeyboardMarkup createDeveloperTaskMenu() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("📝 Create New Task");
        row1.add("🔄 My Active Tasks");
        keyboard.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add("🏃‍♂️ Sprint Management");
        row2.add("✅ Mark Task Complete");
        keyboard.add(row2);

        KeyboardRow row3 = new KeyboardRow();
        row3.add("🏠 Main Menu");
        keyboard.add(row3);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    /**
     * Create keyboard for manager task menu
     */
    public static ReplyKeyboardMarkup createManagerTaskMenu() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("📝 Create New Task");
        row1.add("🔄 My Active Tasks");
        keyboard.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add("🏃‍♂️ Sprint Management");
        row2.add("👥 Team Management");
        keyboard.add(row2);

        KeyboardRow row3 = new KeyboardRow();
        row3.add("📊 KPI Dashboard");
        row3.add("📋 List All Tasks");
        keyboard.add(row3);

        KeyboardRow row4 = new KeyboardRow();
        row4.add("🏠 Main Menu");
        keyboard.add(row4);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    /**
     * Create keyboard for employee task menu
     */
    public static ReplyKeyboardMarkup createEmployeeTaskMenu() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("📝 Create New Task");
        row1.add("🔄 My Active Tasks");
        keyboard.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add("✅ Mark Task Complete");
        row2.add("📋 List All Tasks");
        keyboard.add(row2);

        KeyboardRow row3 = new KeyboardRow();
        row3.add("🏠 Main Menu");
        keyboard.add(row3);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }
}