package com.example.autoflow.worker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.example.autoflow.data.AppDatabase;
import com.example.autoflow.data.WorkflowEntity;
import com.example.autoflow.data.WorkflowRepository;
import com.example.autoflow.model.Action;
import com.example.autoflow.util.Constants;
// Unused imports: org.json.JSONException, org.json.JSONObject, java.util.Calendar
// Can be removed by IDE later

public class TimeTriggerWorker extends Worker {
    private static final String TAG = "TimeTriggerWorker";
    private WorkflowRepository repository;
    // CHANNEL_ID is accessible from BLETriggerWorker
    public static final String CHANNEL_ID = "AutoFlowChannel"; // Made public for clarity if accessed externally

    public TimeTriggerWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        repository = new WorkflowRepository(AppDatabase.getDatabase(context.getApplicationContext()));
        createNotificationChannel(context); // Instance method, called from constructor
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            // getInputData() is an instance method of Worker
            long targetTimeMillis = getInputData().getLong(Constants.KEY_TIME_TRIGGER, -1);
            if (targetTimeMillis == -1) {
                Log.e(TAG, "No target time provided for TimeTriggerWorker");
                return Result.failure();
            }

            long currentTimeMillis = java.util.Calendar.getInstance().getTimeInMillis(); // Explicit Calendar
            long timeDiff = Math.abs(currentTimeMillis - targetTimeMillis);

            if (currentTimeMillis < targetTimeMillis || timeDiff > Constants.TIME_WINDOW_MS) {
                if (currentTimeMillis < targetTimeMillis) {
                    Log.d(TAG, "Target time not reached. Retrying.");
                    return Result.retry();
                } else {
                    Log.d(TAG, "Missed time window. Failing.");
                    return Result.failure();
                }
            }

            Log.d(TAG, "Target time matched. Executing actions.");
            executeActions(); // Instance method
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error in TimeTriggerWorker: " + e.getMessage(), e);
            return Result.failure();
        }
    }

    private void executeActions() {
        long workflowId = getInputData().getLong(Constants.KEY_WORKFLOW_ID, -1);
        if (workflowId == -1) {
            Log.e(TAG, "No workflow ID provided");
            return;
        }

        WorkflowEntity workflowEntity = repository.getWorkflowEntityForActionExecution(workflowId);
        if (workflowEntity != null) {
            Action action = workflowEntity.toAction(); // Using method from WorkflowEntity
            if (action != null) {
                performAction(action); // Instance method
                Log.d(TAG, "Action executed for workflow ID: " + workflowId);
            } else {
                Log.e(TAG, "Action null for workflow ID: " + workflowId);
            }
        } else {
            Log.e(TAG, "No WorkflowEntity found for ID: " + workflowId);
        }
    }

    private void performAction(Action action) {
        Context context = getApplicationContext(); // Instance method from Worker
        switch (action.getType()) {
            case Constants.ACTION_TOGGLE_WIFI:
                Log.d(TAG, "Toggling Wi-Fi to: " + action.getValue());
                break;
            case Constants.ACTION_SEND_NOTIFICATION:
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID) // Uses static CHANNEL_ID
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle(action.getTitle())
                        .setContentText(action.getMessage())
                        .setPriority(getPriorityLevel(action.getPriority())) // Calls static getPriorityLevel
                        .setAutoCancel(true);
                notificationManager.notify((int) System.currentTimeMillis(), builder.build());
                Log.d(TAG, "Notification sent");
                break;
            default:
                Log.w(TAG, "Unsupported action type: " + action.getType());
        }
    }

    // Changed to public static so it can be accessed from BLETriggerWorker
    public static int getPriorityLevel(String priority) {
        if (priority == null) return NotificationCompat.PRIORITY_DEFAULT; // Handle null priority
        switch (priority.toLowerCase()) {
            case "high": return NotificationCompat.PRIORITY_HIGH;
            case "low": return NotificationCompat.PRIORITY_LOW;
            default: return NotificationCompat.PRIORITY_DEFAULT;
        }
    }

    // This method needs context, so it remains an instance method, called from constructor
    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "AutoFlow Notifications", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) { // Good practice to check for null
                manager.createNotificationChannel(channel);
            }
        }
    }
}