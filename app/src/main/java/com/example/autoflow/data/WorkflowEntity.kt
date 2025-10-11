package com.example.autoflow.data;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import org.json.JSONException;
import org.json.JSONObject;

import com.example.autoflow.model.Action;
import com.example.autoflow.model.Trigger;
import com.example.autoflow.util.Constants;
import java.util.Objects;

@Entity(tableName = "workflows")
public class WorkflowEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "workflow_name")
    @NonNull
    private String workflowName = "";

    @ColumnInfo(name = "is_enabled")
    private boolean isEnabled = false;

    @ColumnInfo(name = "trigger_details")
    @NonNull
    private String triggerDetails = "";

    @ColumnInfo(name = "action_details")
    @NonNull
    private String actionDetails = "";

    // Default constructor required by Room
    public WorkflowEntity() {
        this.workflowName = "";
        this.triggerDetails = "";
        this.actionDetails = "";
    }

    @Ignore
    public WorkflowEntity(@NonNull String workflowName, boolean isEnabled,
                          @NonNull String triggerDetails, @NonNull String actionDetails) {
        this.workflowName = Objects.requireNonNull(workflowName, "Workflow name cannot be null");
        this.isEnabled = isEnabled;
        this.triggerDetails = Objects.requireNonNull(triggerDetails, "Trigger details cannot be null");
        this.actionDetails = Objects.requireNonNull(actionDetails, "Action details cannot be null");
    }

    // Getters and Setters with null safety
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getWorkflowName() {
        return workflowName != null ? workflowName : "";
    }

    public void setWorkflowName(@Nullable String workflowName) {
        this.workflowName = workflowName != null ? workflowName : "";
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
    }

    @NonNull
    public String getTriggerDetails() {
        return triggerDetails != null ? triggerDetails : "";
    }

    public void setTriggerDetails(@Nullable String triggerDetails) {
        this.triggerDetails = triggerDetails != null ? triggerDetails : "";
    }

    @NonNull
    public String getActionDetails() {
        return actionDetails != null ? actionDetails : "";
    }

    public void setActionDetails(@Nullable String actionDetails) {
        this.actionDetails = actionDetails != null ? actionDetails : "";
    }

    // In WorkflowEntity.java
    public static WorkflowEntity fromTriggerAndAction(
            @NonNull String workflowName,
            boolean isEnabled,
            @NonNull com.example.autoflow.model.Trigger trigger,  // ← Full package name
            @NonNull com.example.autoflow.model.Action action) {   // ← Full package name

        try {
            Log.d("WorkflowEntity", "🔵 fromTriggerAndAction called");

            // Convert Trigger to JSON
            JSONObject triggerJson = new JSONObject();
            triggerJson.put("type", trigger.type);
            triggerJson.put("value", trigger.value);
            String triggerDetails = triggerJson.toString();

            // Convert Action to JSON
            JSONObject actionJson = new JSONObject();
            actionJson.put("type", action.type);
            if (action.title != null) actionJson.put("title", action.title);
            if (action.message != null) actionJson.put("message", action.message);
            if (action.priority != null) actionJson.put("priority", action.priority);
            if (action.getValue() != null) actionJson.put("value", action.getValue());
            String actionDetails = actionJson.toString();

            // Create entity
            WorkflowEntity entity = new WorkflowEntity();
            entity.setWorkflowName(workflowName);
            entity.setTriggerDetails(triggerDetails);
            entity.setActionDetails(actionDetails);
            entity.setEnabled(isEnabled);

            Log.d("WorkflowEntity", "✅ WorkflowEntity created");
            return entity;

        } catch (Exception e) {
            Log.e("WorkflowEntity", "❌ Error", e);
            return null;
        }
    }


    @Nullable
    public com.example.autoflow.model.Trigger toTrigger() {
        // Null and empty string safety check
        if (triggerDetails == null || triggerDetails.trim().isEmpty()) {
            return null;
        }

        try {
            JSONObject json = new JSONObject(triggerDetails);

            // Extract values with null safety
            long id = json.optLong("id", 0L);
            long workflowId = json.optLong("workflowId", 0L);
            String type = json.optString("type");
            String value = json.optString("value", "");

            // Validate essential fields
            if (type == null || type.trim().isEmpty()) {
                System.err.println("Trigger type is null or empty, cannot create Trigger object");
                return null;
            }

            // Create trigger with null-safe values
            return new com.example.autoflow.model.Trigger(id, workflowId, type.trim(), value);

        } catch (JSONException e) {
            System.err.println("Error parsing trigger JSON: " + e.getMessage());
            return null;
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid trigger data: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("Unexpected error creating trigger: " + e.getMessage());
            return null;
        }
    }

    @Nullable
    public com.example.autoflow.model.Action toAction() {
        // Null and empty string safety check
        if (actionDetails == null || actionDetails.trim().isEmpty()) {
            return null;
        }

        try {
            JSONObject json = new JSONObject(actionDetails);
            String type = json.optString("type");

            // Validate essential type field
            if (type == null || type.trim().isEmpty()) {
                System.err.println("Action type is null or empty, cannot create Action object");
                return null;
            }

            com.example.autoflow.model.Action action;
            type = type.trim();

            // Handle notification action with null safety
            if (Constants.ACTION_SEND_NOTIFICATION.equals(type) &&
                    json.has(Constants.JSON_KEY_NOTIFICATION_TITLE) &&
                    json.has(Constants.JSON_KEY_NOTIFICATION_MESSAGE) &&
                    json.has(Constants.JSON_KEY_NOTIFICATION_PRIORITY)) {

                String title = json.optString(Constants.JSON_KEY_NOTIFICATION_TITLE, "");
                String message = json.optString(Constants.JSON_KEY_NOTIFICATION_MESSAGE, "");
                String priority = json.optString(Constants.JSON_KEY_NOTIFICATION_PRIORITY, "Normal");

                // Ensure no null values are passed to constructor
                action = new com.example.autoflow.model.Action(type,
                        title.isEmpty() ? "Default Title" : title,
                        message.isEmpty() ? "Default Message" : message,
                        priority.isEmpty() ? "Normal" : priority);
            } else {
                // Create basic action with type only
                action = new com.example.autoflow.model.Action(type);
            }

            // Set value if present and not null
            if (json.has("value") && !json.isNull("value")) {
                String value = json.optString("value");
                if (value != null && !value.trim().isEmpty()) {
                    action.setValue(value.trim());
                }
            }

            return action;

        } catch (JSONException e) {
            System.err.println("Error parsing action JSON: " + e.getMessage());
            return null;
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid action data: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("Unexpected error creating action: " + e.getMessage());
            return null;
        }
    }
    // Helper class for returning two strings (Java equivalent of Kotlin Pair)

    // Override equals and hashCode for proper object comparison
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkflowEntity that = (WorkflowEntity) o;
        return id == that.id &&
                isEnabled == that.isEnabled &&
                Objects.equals(workflowName, that.workflowName) &&
                Objects.equals(triggerDetails, that.triggerDetails) &&
                Objects.equals(actionDetails, that.actionDetails);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, workflowName, isEnabled, triggerDetails, actionDetails);
    }

    @Override
    @NonNull
    public String toString() {
        return "WorkflowEntity{" +
                "id=" + id +
                ", workflowName='" + workflowName + '\'' +
                ", isEnabled=" + isEnabled +
                ", triggerDetails='" + triggerDetails + '\'' +
                ", actionDetails='" + actionDetails + '\'' +
                '}';
    }
}
