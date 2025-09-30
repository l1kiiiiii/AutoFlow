package com.example.autoflow.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import org.json.JSONException;
import org.json.JSONObject;
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

    @NonNull
    public static WorkflowEntity fromTriggerAndAction(@Nullable String workflowName,
                                                      boolean isEnabled,
                                                      @Nullable com.example.autoflow.model.Trigger trigger,
                                                      @Nullable com.example.autoflow.model.Action actionInput) {
        // Null-safe handling for workflowName
        String safeName = workflowName != null && !workflowName.trim().isEmpty() ? workflowName : "Unnamed Workflow";

        JSONObject triggerJson = new JSONObject();
        if (trigger != null) {
            try {
                triggerJson.put("id", trigger.getId());
                triggerJson.put("workflowId", trigger.getWorkflowId());
                triggerJson.put("type", trigger.getType() != null ? trigger.getType() : "");
                triggerJson.put("value", trigger.getValue() != null ? trigger.getValue() : "");
            } catch (JSONException e) {
                // Log error but don't throw - create empty JSON object instead
                System.err.println("Error creating trigger JSON: " + e.getMessage());
                triggerJson = new JSONObject();
            }
        }

        JSONObject actionJson = new JSONObject();
        if (actionInput != null) {
            try {
                String actionType = actionInput.getType() != null ? actionInput.getType() : "";
                actionJson.put("type", actionType);

                // Only add non-null values to JSON
                if (actionInput.getTitle() != null && !actionInput.getTitle().isEmpty()) {
                    actionJson.put(Constants.JSON_KEY_NOTIFICATION_TITLE, actionInput.getTitle());
                }
                if (actionInput.getMessage() != null && !actionInput.getMessage().isEmpty()) {
                    actionJson.put(Constants.JSON_KEY_NOTIFICATION_MESSAGE, actionInput.getMessage());
                }
                if (actionInput.getPriority() != null && !actionInput.getPriority().isEmpty()) {
                    actionJson.put(Constants.JSON_KEY_NOTIFICATION_PRIORITY, actionInput.getPriority());
                }
                if (actionInput.getValue() != null && !actionInput.getValue().isEmpty()) {
                    actionJson.put("value", actionInput.getValue());
                }
            } catch (JSONException e) {
                // Log error but don't throw - create empty JSON object instead
                System.err.println("Error creating action JSON: " + e.getMessage());
                actionJson = new JSONObject();
            }
        }

        return new WorkflowEntity(safeName, isEnabled,
                triggerJson.toString(),
                actionJson.toString());
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
