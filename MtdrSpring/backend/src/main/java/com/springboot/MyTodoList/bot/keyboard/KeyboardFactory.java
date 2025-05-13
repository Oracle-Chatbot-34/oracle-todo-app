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
     * Create the main menu keyboard
     */
    public static ReplyKeyboardMarkup createMainMenuKeyboard(User user) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> keyboard = new ArrayList<>();

        // First row with main actions
        KeyboardRow row = new KeyboardRow();
        row.add("📝 List All Tasks");
        row.add("📝 Create New Task");
        keyboard.add(row);

        // Second row depends on user role
        if (user.isDeveloper() || user.isManager()) {
            row = new KeyboardRow();
            row.add("🔄 My Active Tasks");
            row.add("📊 Sprint Board");
            keyboard.add(row);
        }

        // Third row with additional options for managers
        if (user.isManager()) {
            row = new KeyboardRow();
            row.add("👥 Team Management");
            row.add("📅 Sprint Management");
            keyboard.add(row);
        }

        // Last row with hide option
        row = new KeyboardRow();
        row.add("❌ Hide Keyboard");
        keyboard.add(row);

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
        keyboard.add(addRow);

        KeyboardRow titleRow = new KeyboardRow();
        titleRow.add("MY TASK LIST");
        keyboard.add(titleRow);

        // Active tasks
        List<ToDoItem> activeTasks = new ArrayList<>();
        List<ToDoItem> completedTasks = new ArrayList<>();

        for (ToDoItem task : tasks) {
            if (task.isDone()) {
                completedTasks.add(task);
            } else {
                activeTasks.add(task);
            }
        }

        // Add active tasks
        for (ToDoItem task : activeTasks) {
            KeyboardRow row = new KeyboardRow();
            row.add(task.getTitle());
            row.add(task.getID() + "-DONE");
            keyboard.add(row);
        }

        // Add completed tasks
        for (ToDoItem task : completedTasks) {
            KeyboardRow row = new KeyboardRow();
            row.add(task.getTitle());
            row.add(task.getID() + "-UNDO");
            row.add(task.getID() + "-DELETE");
            keyboard.add(row);
        }

        // Add footer row
        KeyboardRow footerRow = new KeyboardRow();
        footerRow.add("🏠 Main Menu");
        keyboard.add(footerRow);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    /**
     * Create keyboard for sprint management
     */
    public static ReplyKeyboardMarkup createSprintManagementKeyboard(boolean hasActiveSprint, boolean isManager) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add("📊 View Sprint Board");
        row.add("🔍 View All Sprints");
        keyboard.add(row);

        if (isManager) {
            row = new KeyboardRow();
            row.add("🆕 Create New Sprint");

            if (hasActiveSprint) {
                row.add("⏹️ End Active Sprint");
            }

            keyboard.add(row);
        }

        row = new KeyboardRow();
        row.add("🏠 Main Menu");
        keyboard.add(row);

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
            row.add("ID: " + task.getID() + " - " + task.getTitle());
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
     * Create keyboard for after task completion
     */
    public static ReplyKeyboardMarkup createAfterTaskCompletionKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add("🔄 My Active Tasks");
        row.add("📊 Sprint Board");
        keyboard.add(row);

        row = new KeyboardRow();
        row.add("🏠 Main Menu");
        keyboard.add(row);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    /**
     * Create empty keyboard (remove keyboard)
     */
    public static ReplyKeyboardRemove createEmptyKeyboard() {
        return new ReplyKeyboardRemove(true);
    }
}