package com.springboot.MyTodoList.bot.handler;

import com.springboot.MyTodoList.bot.keyboard.KeyboardFactory;
import com.springboot.MyTodoList.bot.service.BotService;
import com.springboot.MyTodoList.bot.util.BotLogger;
import com.springboot.MyTodoList.model.ToDoItem;
import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.model.bot.UserBotState;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Handler for task creation workflow
 */
public class TaskCreationHandler {
    private final BotLogger logger = new BotLogger(TaskCreationHandler.class);
    private final BotService botService;
    private final TelegramLongPollingBot bot;

    // Animation frames for loading indicators
    private static final String[] LOADING_FRAMES = { "⬜⬜⬜", "⬛⬜⬜", "⬛⬛⬜", "⬛⬛⬛", "✅" };

    public TaskCreationHandler(BotService botService, TelegramLongPollingBot bot) {
        this.botService = botService;
        this.bot = bot;
    }

    /**
     * Enhanced method to start task creation with animation
     */
    public void startTaskCreation(long chatId, UserBotState state) {
        logger.info(chatId, "Starting task creation flow for user: {}", state.getUser().getFullName());
        try {
            // Show loading animation
            Message loadingMessage = MessageHandler.sendAnimatedLoadingMessage(chatId, "Preparing task creation...",
                    bot);

            if (loadingMessage == null) {
                MessageHandler.sendErrorMessage(chatId, "Failed to start task creation. Please try again.", bot);
                return;
            }

            int messageId = loadingMessage.getMessageId();

            // Animation frames
            for (int i = 1; i <= 4; i++) {
                MessageHandler.updateLoadingAnimation(chatId, messageId, "Preparing task creation...", i, bot);
            }

            // Set state to task creation mode
            state.setNewTaskMode(true);
            state.setTaskCreationStage("TITLE");
            logger.debug(chatId, "Set user state: newTaskMode=true, taskCreationStage=TITLE");

            // Update the message with the initial task creation prompt
            EditMessageText finalMessage = new EditMessageText();
            finalMessage.setChatId(chatId);
            finalMessage.setMessageId(messageId);
            finalMessage.setText("Let's create a new task. First, please enter the task title:");
            finalMessage.enableHtml(true);
            bot.execute(finalMessage);

            // Create keyboard for easier navigation
            SendMessage keyboardMessage = new SendMessage();
            keyboardMessage.setChatId(chatId);
            keyboardMessage.setText("Type the task title below or select 'Cancel' to abort:");

            // Add Cancel option in keyboard
            ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
            keyboardMarkup.setResizeKeyboard(true);
            keyboardMarkup.setOneTimeKeyboard(false);
            List<KeyboardRow> keyboard = new ArrayList<>();

            KeyboardRow row = new KeyboardRow();
            row.add("Cancel");
            keyboard.add(row);

            keyboardMarkup.setKeyboard(keyboard);
            keyboardMessage.setReplyMarkup(keyboardMarkup);

            bot.execute(keyboardMessage);
            logger.info(chatId, "Task creation prompt sent successfully");
        } catch (Exception e) {
            logger.error(chatId, "Error starting task creation", e);
            MessageHandler.sendErrorMessage(chatId, "There was a problem starting task creation. Please try again.",
                    bot);
        }
    }

    /**
     * Start simple item creation (for non-developer users)
     */
    public void startSimpleItemCreation(long chatId, UserBotState state) {
        logger.info(chatId, "Starting simple item creation for user: {}", state.getUser().getFullName());
        try {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("Please type a new todo item and press send:");

            // Hide keyboard
            message.setReplyMarkup(KeyboardFactory.createEmptyKeyboard());

            // Set state
            state.setNewTaskMode(true);
            state.setTaskCreationStage("SIMPLE");
            logger.debug(chatId, "Set state: newTaskMode=true, stage=SIMPLE");

            bot.execute(message);
            logger.info(chatId, "Simple item creation prompt sent");
        } catch (Exception e) {
            logger.error(chatId, "Error initiating simple item creation", e);
            MessageHandler.sendErrorMessage(chatId, "Failed to start item creation. Please try again later.", bot);
        }
    }

    /**
     * Enhanced process task creation with better state management
     */
    public void processTaskCreation(long chatId, String messageText, UserBotState state) {
        logger.info(chatId, "Processing task creation, stage: {}", state.getTaskCreationStage());
        logger.debug(chatId, "Task creation input: '{}'", messageText);
        try {
            // Check for cancellation
            if (messageText.equals("Cancel")) {
                state.resetTaskCreation();
                MessageHandler.sendMessage(chatId, "Task creation cancelled.", bot);
                MessageHandler.showMainScreen(chatId, state, bot);
                return;
            }

            String stage = state.getTaskCreationStage();

            if (stage == null) {
                logger.warn(chatId, "Task creation stage is null, assuming TITLE stage");
                stage = "TITLE";
                state.setTaskCreationStage(stage);
            }

            // Process simple task creation (for non-developer users)
            if ("SIMPLE".equals(stage)) {
                processSimpleTaskCreation(chatId, messageText, state);
                return;
            }

            // Process full task creation workflow
            switch (stage) {
                case "TITLE":
                    processTaskTitle(chatId, messageText, state);
                    break;
                case "DESCRIPTION":
                    processTaskDescription(chatId, messageText, state);
                    break;
                case "ESTIMATED_HOURS":
                    processEstimatedHours(chatId, messageText, state);
                    break;
                case "ASSIGNEE":
                    processAssignee(chatId, messageText, state);
                    break;
                case "PRIORITY":
                    processPriority(chatId, messageText, state);
                    break;
                case "CONFIRMATION":
                    processConfirmation(chatId, messageText, state);
                    break;
                default:
                    logger.warn(chatId, "Unknown task creation stage: {}", stage);
                    MessageHandler.sendErrorMessage(chatId,
                            "An error occurred in the task creation process. Please try again.", bot);
                    state.resetTaskCreation();
                    MessageHandler.showMainScreen(chatId, state, bot);
            }
        } catch (Exception e) {
            logger.error(chatId, "Error in task creation process", e);
            MessageHandler.sendErrorMessage(chatId,
                    "There was an error in the task creation process. Please try again.", bot);

            // Reset task creation state
            state.resetTaskCreation();
            MessageHandler.showMainScreen(chatId, state, bot);
        }
    }

    /**
     * Process simple task creation
     */
    private void processSimpleTaskCreation(long chatId, String messageText, UserBotState state) {
        logger.info(chatId, "Processing simple task creation: '{}'", messageText);
        try {
            // Show loading animation
            SendMessage loadingMessage = new SendMessage();
            loadingMessage.setChatId(chatId);
            loadingMessage.setText("Creating task...\n⬜⬜⬜");
            loadingMessage.enableHtml(true);

            Message sentMessage = bot.execute(loadingMessage);

            // Animation frames
            for (int i = 1; i < LOADING_FRAMES.length - 1; i++) {
                Thread.sleep(300);
                EditMessageText updateMessage = new EditMessageText();
                updateMessage.setChatId(chatId);
                updateMessage.setMessageId(sentMessage.getMessageId());
                updateMessage.setText("Creating task...\n" + LOADING_FRAMES[i]);
                updateMessage.enableHtml(true);
                bot.execute(updateMessage);
            }

            ToDoItem newItem = new ToDoItem();
            newItem.setDescription(messageText);
            newItem.setTitle(messageText.length() > 50 ? messageText.substring(0, 50) : messageText);
            newItem.setCreationTs(OffsetDateTime.now());
            newItem.setDone(false);

            // If user is associated with a team, set the team ID
            if (state.getUser().getTeam() != null) {
                logger.debug(chatId, "Setting team ID {} for new item", state.getUser().getTeam().getId());
                newItem.setTeamId(state.getUser().getTeam().getId());
            }

            // Set the user as assignee
            logger.debug(chatId, "Setting assignee ID {} for new item", state.getUser().getId());
            newItem.setAssigneeId(state.getUser().getId());

            // Add the item
            ToDoItem addedItem = botService.addToDoItem(newItem);
            logger.info(chatId, "Successfully added new item with ID: {}", addedItem.getID());

            // Reset state
            state.resetTaskCreation();
            logger.debug(chatId, "Reset task creation state");

            // Confirm to the user
            EditMessageText successMessage = new EditMessageText();
            successMessage.setChatId(chatId);
            successMessage.setMessageId(sentMessage.getMessageId());
            successMessage.setText("✅ New item added successfully!" +
                    "\n\nID: " + addedItem.getID() +
                    "\nTitle: " + addedItem.getTitle());
            successMessage.enableHtml(true);

            bot.execute(successMessage);
            logger.info(chatId, "Task creation success message sent");

            // Return to main menu after a short delay
            Thread.sleep(1500);
            MessageHandler.showMainScreen(chatId, state, bot);
        } catch (Exception e) {
            logger.error(chatId, "Error adding simple task", e);
            MessageHandler.sendErrorMessage(chatId, "Failed to add item. Please try again later.", bot);

            // Reset state
            state.resetTaskCreation();
        }
    }

    /**
     * Process task title input with better state tracking
     */
    private void processTaskTitle(long chatId, String messageText, UserBotState state) {
        // Validate task title
        if (messageText.length() > 100) {
            MessageHandler.sendMessage(chatId, "Task title is too long (max 100 characters). Please try again:", bot);
            return;
        }

        if (messageText.trim().isEmpty()) {
            MessageHandler.sendMessage(chatId, "Task title cannot be empty. Please enter a title:", bot);
            return;
        }

        logger.debug(chatId, "Storing task title: '{}'", messageText);
        state.setTempTaskTitle(messageText);
        state.setTaskCreationStage("DESCRIPTION");
        logger.debug(chatId, "Updated stage to DESCRIPTION");

        try {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("Great! Now please provide a description for the task:");

            // Keep the Cancel option
            ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
            keyboardMarkup.setResizeKeyboard(true);
            List<KeyboardRow> keyboard = new ArrayList<>();

            KeyboardRow row = new KeyboardRow();
            row.add("Cancel");
            keyboard.add(row);

            keyboardMarkup.setKeyboard(keyboard);
            message.setReplyMarkup(keyboardMarkup);

            bot.execute(message);
            logger.info(chatId, "Description prompt sent");
        } catch (TelegramApiException e) {
            logger.error(chatId, "Error sending description prompt", e);
            throw new RuntimeException("Failed to send message", e);
        }
    }

    /**
     * Process task description
     */
    private void processTaskDescription(long chatId, String messageText, UserBotState state) {
        logger.debug(chatId, "Storing task description: '{}'", messageText);
        state.setTempTaskDescription(messageText);
        state.setTaskCreationStage("ESTIMATED_HOURS");
        logger.debug(chatId, "Updated stage to ESTIMATED_HOURS");

        try {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("Please enter the estimated hours to complete this task (must be 4 hours or less):");
            bot.execute(message);
            logger.info(chatId, "Estimated hours prompt sent");
        } catch (TelegramApiException e) {
            logger.error(chatId, "Error sending estimated hours prompt", e);
            throw new RuntimeException("Failed to send message", e);
        }
    }

    /**
     * Process estimated hours
     */
    private void processEstimatedHours(long chatId, String messageText, UserBotState state) {
        try {
            double estimatedHours = Double.parseDouble(messageText);
            if (estimatedHours <= 0 || estimatedHours > 4) {
                throw new IllegalArgumentException("Estimated hours must be between 0 and 4.");
            }

            state.setTempEstimatedHours(estimatedHours);
            logger.debug(chatId, "Stored estimated hours: {}", estimatedHours);

            if (state.getUser().isManager()) {
                // Managers can assign tasks to others
                moveToAssigneeSelection(chatId, state);
            } else {
                // Developers assign tasks to themselves, skip to priority
                moveToPrioritySelection(chatId, state);
            }
        } catch (NumberFormatException e) {
            logger.warn(chatId, "Invalid estimated hours entered: '{}'", messageText);
            try {
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("Please enter a valid number for estimated hours (between 0 and 4):");
                bot.execute(message);
            } catch (TelegramApiException ex) {
                logger.error(chatId, "Error sending validation error message", ex);
            }
        } catch (IllegalArgumentException e) {
            logger.warn(chatId, "Invalid estimated hours value: '{}'", messageText);
            try {
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText(e.getMessage() + " Please enter a valid number:");
                bot.execute(message);
            } catch (TelegramApiException ex) {
                logger.error(chatId, "Error sending validation error message", ex);
            }
        }
    }

    /**
     * Move to assignee selection
     */
    private void moveToAssigneeSelection(long chatId, UserBotState state) {
        state.setTaskCreationStage("ASSIGNEE");
        logger.debug(chatId, "Updated stage to ASSIGNEE");

        try {
            // Show loading animation
            SendMessage loadingMessage = new SendMessage();
            loadingMessage.setChatId(chatId);
            loadingMessage.setText("Loading team members...\n⬜⬜⬜");
            loadingMessage.enableHtml(true);

            Message sentMessage = bot.execute(loadingMessage);

            // Animation frames
            for (int i = 1; i < LOADING_FRAMES.length - 1; i++) {
                Thread.sleep(300);
                EditMessageText updateMessage = new EditMessageText();
                updateMessage.setChatId(chatId);
                updateMessage.setMessageId(sentMessage.getMessageId());
                updateMessage.setText("Loading team members...\n" + LOADING_FRAMES[i]);
                updateMessage.enableHtml(true);
                bot.execute(updateMessage);
            }

            // Get team members for selection
            List<User> teamMembers = botService.findUsersByTeamId(state.getUser().getTeam().getId());
            logger.debug(chatId, "Found {} team members", teamMembers.size());

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("Please select who to assign this task to:");

            // Create keyboard with team members
            message.setReplyMarkup(KeyboardFactory.createTeamMemberSelectionKeyboard(teamMembers, state.getUser()));

            bot.execute(message);
            logger.info(chatId, "Assignee selection prompt sent");
        } catch (Exception e) {
            logger.error(chatId, "Error preparing assignee selection", e);
            throw new RuntimeException("Failed to prepare assignee selection", e);
        }
    }

    /**
     * Move to priority selection
     */
    private void moveToPrioritySelection(long chatId, UserBotState state) {
        state.setTaskCreationStage("PRIORITY");
        logger.debug(chatId, "Updated stage to PRIORITY");

        try {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("Please select the priority for this task:");

            // Create priority selection keyboard
            message.setReplyMarkup(KeyboardFactory.createTaskPriorityKeyboard());

            bot.execute(message);
            logger.info(chatId, "Priority selection prompt sent");
        } catch (TelegramApiException e) {
            logger.error(chatId, "Error sending priority selection prompt", e);
            throw new RuntimeException("Failed to send message", e);
        }
    }

    /**
     * Process assignee selection
     */
    private void processAssignee(long chatId, String messageText, UserBotState state) {
        logger.debug(chatId, "Processing assignee selection: '{}'", messageText);

        Long assigneeId;
        if (messageText.startsWith("Me (")) {
            // Self-assignment
            assigneeId = state.getUser().getId();
            logger.debug(chatId, "Self-assignment selected, assignee ID: {}", assigneeId);
        } else if (messageText.equals("Cancel")) {
            // Cancel task creation
            logger.info(chatId, "Task creation cancelled by user");
            state.resetTaskCreation();
            MessageHandler.sendMessage(chatId, "Task creation cancelled.", bot);
            MessageHandler.showMainScreen(chatId, state, bot);
            return;
        } else {
            // Extract ID from "Name (ID: X)"
            try {
                String idPart = messageText.substring(messageText.indexOf("ID: ") + 4,
                        messageText.length() - 1);
                assigneeId = Long.parseLong(idPart);
                logger.debug(chatId, "Extracted assignee ID from selection: {}", assigneeId);
            } catch (Exception e) {
                logger.warn(chatId, "Error parsing assignee ID from: '{}'", messageText, e);
                MessageHandler.sendErrorMessage(chatId, "Invalid selection. Please select a valid assignee.", bot);
                return;
            }
        }

        // Store assignee ID and move to priority
        state.setTempAssigneeId(assigneeId);
        moveToPrioritySelection(chatId, state);
    }

    /**
     * Process priority selection
     */
    private void processPriority(long chatId, String messageText, UserBotState state) {
        logger.debug(chatId, "Processing priority selection: '{}'", messageText);
        if (!messageText.equals("High") && !messageText.equals("Medium") && !messageText.equals("Low")) {
            logger.warn(chatId, "Invalid priority entered: '{}'", messageText);
            try {
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("Please select a valid priority (High, Medium, or Low):");
                bot.execute(message);
            } catch (TelegramApiException e) {
                logger.error(chatId, "Error sending validation error message", e);
            }
            return;
        }

        state.setTempPriority(messageText);
        state.setTaskCreationStage("CONFIRMATION");
        logger.debug(chatId, "Updated stage to CONFIRMATION, stored priority: {}", messageText);

        try {
            // Build confirmation message
            StringBuilder summary = new StringBuilder();
            summary.append("Please confirm the task details:\n\n");
            summary.append("Title: ").append(state.getTempTaskTitle()).append("\n");
            summary.append("Description: ").append(state.getTempTaskDescription()).append("\n");
            summary.append("Estimated Hours: ").append(state.getTempEstimatedHours()).append("\n");
            summary.append("Priority: ").append(state.getTempPriority()).append("\n");

            // Add assignee information
            if (state.getTempAssigneeId() != null) {
                Optional<User> assignee = botService.findUserById(state.getTempAssigneeId());
                if (assignee.isPresent()) {
                    summary.append("Assignee: ").append(assignee.get().getFullName()).append("\n");
                }
            } else {
                summary.append("Assignee: ").append(state.getUser().getFullName()).append(" (you)\n");
            }

            summary.append("\nIs this correct?");
            logger.debug(chatId, "Task confirmation summary created");

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(summary.toString());
            message.setReplyMarkup(KeyboardFactory.createTaskConfirmationKeyboard());

            bot.execute(message);
            logger.info(chatId, "Confirmation prompt sent");
        } catch (TelegramApiException e) {
            logger.error(chatId, "Error sending confirmation prompt", e);
            throw new RuntimeException("Failed to send message", e);
        }
    }

    /**
     * Process confirmation
     */
    private void processConfirmation(long chatId, String messageText, UserBotState state) {
        if (messageText.equals("Yes, create task")) {
            createTask(chatId, state);
        } else {
            // Cancel task creation
            logger.info(chatId, "Task creation cancelled by user");
            state.resetTaskCreation();

            try {
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("Task creation cancelled. What would you like to do next?");

                // Create keyboard with options
                ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                keyboardMarkup.setResizeKeyboard(true);
                List<KeyboardRow> keyboard = new ArrayList<>();

                KeyboardRow row1 = new KeyboardRow();
                row1.add("📝 Create New Task");
                row1.add("🔄 My Active Tasks");
                keyboard.add(row1);

                KeyboardRow row2 = new KeyboardRow();
                row2.add("🏠 Main Menu");
                keyboard.add(row2);

                keyboardMarkup.setKeyboard(keyboard);
                message.setReplyMarkup(keyboardMarkup);

                bot.execute(message);
                logger.info(chatId, "Task creation cancellation message sent");
            } catch (TelegramApiException e) {
                logger.error(chatId, "Error sending cancellation message", e);
            }
        }
    }

    /**
     * Create a task with detailed animation progress
     */
    private void createTask(long chatId, UserBotState state) {
        try {
            // Show loading animation
            Message loadingMessage = MessageHandler.sendAnimatedLoadingMessage(chatId, "Creating task...", bot);

            if (loadingMessage == null) {
                MessageHandler.sendErrorMessage(chatId, "Failed to start task creation. Please try again.", bot);
                state.resetTaskCreation();
                return;
            }

            int messageId = loadingMessage.getMessageId();

            // Animation frames with specific step descriptions
            String[] stepTexts = {
                    "Creating task...\nValidating input",
                    "Creating task...\nPreparing task data",
                    "Creating task...\nSaving to database"
            };

            for (int i = 0; i < stepTexts.length; i++) {
                MessageHandler.updateLoadingAnimation(chatId, messageId, stepTexts[i], i + 1, bot);
            }

            // Create the task
            ToDoItem task = new ToDoItem();
            task.setTitle(state.getTempTaskTitle());
            task.setDescription(state.getTempTaskDescription());
            task.setEstimatedHours(state.getTempEstimatedHours());
            task.setPriority(state.getTempPriority());
            task.setCreationTs(OffsetDateTime.now());

            // Set assignee (from temp state or current user)
            if (state.getTempAssigneeId() != null) {
                task.setAssigneeId(state.getTempAssigneeId());
                logger.debug(chatId, "Setting assignee from temp state: {}", state.getTempAssigneeId());
            } else {
                task.setAssigneeId(state.getUser().getId());
                logger.debug(chatId, "Setting current user as assignee: {}", state.getUser().getId());
            }

            // Set team ID if user is in a team
            if (state.getUser().getTeam() != null) {
                task.setTeamId(state.getUser().getTeam().getId());
                logger.debug(chatId, "Setting team ID: {}", state.getUser().getTeam().getId());
            }

            ToDoItem savedTask = botService.addToDoItem(task);
            logger.info(chatId, "Task created successfully with ID: {}", savedTask.getID());

            // Final animation step
            MessageHandler.updateLoadingAnimation(chatId, messageId, "Creating task...\nTask created successfully!", 4,
                    bot);
            Thread.sleep(500); // Brief pause for user to see the success animation

            // Show success message with task details
            StringBuilder successMessage = new StringBuilder();
            successMessage.append("✅ <b>Task created successfully!</b>\n\n");
            successMessage.append("<b>ID:</b> ").append(savedTask.getID()).append("\n");
            successMessage.append("<b>Title:</b> ").append(savedTask.getTitle()).append("\n");

            if (savedTask.getPriority() != null) {
                successMessage.append("<b>Priority:</b> ").append(savedTask.getPriority()).append("\n");
            }

            if (savedTask.getEstimatedHours() != null) {
                successMessage.append("<b>Estimated Hours:</b> ").append(savedTask.getEstimatedHours()).append("\n");
            }

            successMessage.append("\nWhat would you like to do next?");

            // Reset state
            state.resetTaskCreation();
            logger.debug(chatId, "Reset task creation state");

            // Update the message with success content
            EditMessageText finalMessage = new EditMessageText();
            finalMessage.setChatId(chatId);
            finalMessage.setMessageId(messageId);
            finalMessage.setText(successMessage.toString());
            finalMessage.enableHtml(true);
            bot.execute(finalMessage);

            // Send keyboard options in a new message
            SendMessage optionsMessage = new SendMessage();
            optionsMessage.setChatId(chatId);
            optionsMessage.setText("Select an option:");

            // Create a properly initialized keyboard
            ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
            keyboardMarkup.setResizeKeyboard(true);
            List<KeyboardRow> keyboard = new ArrayList<>();

            // Add rows with buttons
            KeyboardRow row1 = new KeyboardRow();
            row1.add("📝 Create New Task");
            row1.add("🔄 My Active Tasks");
            keyboard.add(row1);

            KeyboardRow row2 = new KeyboardRow();
            row2.add("📝 List All Tasks");
            row2.add("🏃‍♂️ Sprint Management");
            keyboard.add(row2);

            KeyboardRow row3 = new KeyboardRow();
            row3.add("🏠 Main Menu");
            keyboard.add(row3);

            // Set the keyboard to the markup
            keyboardMarkup.setKeyboard(keyboard);
            optionsMessage.setReplyMarkup(keyboardMarkup);

            bot.execute(optionsMessage);
            logger.info(chatId, "Post-task creation options presented");
        } catch (Exception e) {
            logger.error(chatId, "Error creating task", e);

            try {
                // Create a safe error message with a valid keyboard
                SendMessage errorMessage = new SendMessage();
                errorMessage.setChatId(chatId);
                errorMessage.setText("❌ There was an error creating the task. Please try again.");

                // Always provide a valid keyboard for error messages
                ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                keyboardMarkup.setResizeKeyboard(true);
                List<KeyboardRow> keyboard = new ArrayList<>();

                KeyboardRow row = new KeyboardRow();
                row.add("🏠 Main Menu");
                keyboard.add(row);

                keyboardMarkup.setKeyboard(keyboard);
                errorMessage.setReplyMarkup(keyboardMarkup);

                bot.execute(errorMessage);
            } catch (Exception ex) {
                // If even the error handling fails, log it but don't throw
                logger.error(chatId, "Failed to send error message", ex);
            }

            state.resetTaskCreation();
        }
    }
}