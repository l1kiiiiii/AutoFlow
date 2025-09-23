package com.example.autoflow.data;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import org.json.JSONException;
import org.json.JSONObject;

@Entity(tableName = "workflows")
public class WorkflowEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "workflow_name")
    private String workflowName;

    @ColumnInfo(name = "is_enabled")
    private boolean isEnabled;

    @ColumnInfo(name = "trigger_details")
    private String triggerDetails;

    @ColumnInfo(name = "action_details")
    private String actionDetails;

    public WorkflowEntity() {}

    public WorkflowEntity(String workflowName, boolean isEnabled, String triggerDetails, String actionDetails) {
        this.workflowName = workflowName;
        this.isEnabled = isEnabled;
        this.triggerDetails = triggerDetails;
        this.actionDetails = actionDetails;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getWorkflowName() { return workflowName; }
    public void setWorkflowName(String workflowName) { this.workflowName = workflowName; }
    public boolean isEnabled() { return isEnabled; }
    public void setEnabled(boolean enabled) { this.isEnabled = enabled; }
    public String getTriggerDetails() { return triggerDetails; }
    public void setTriggerDetails(String triggerDetails) { this.triggerDetails = triggerDetails; }
    public String getActionDetails() { return actionDetails; }
    public void setActionDetails(String actionDetails) { this.actionDetails = actionDetails; }

    public static WorkflowEntity fromTriggerAndAction(String workflowName, boolean isEnabled, com.example.autoflow.model.Trigger trigger, com.example.autoflow.model.Action action) {
        JSONObject triggerJson = new JSONObject();
        try {
            triggerJson.put("id", trigger.getId());
            triggerJson.put("workflowId", trigger.getWorkflowId());
            triggerJson.put("type", trigger.getType());
            triggerJson.put("value", trigger.getValue());
        } catch (JSONException e) { e.printStackTrace(); }

        JSONObject actionJson = new JSONObject();
        try {
            actionJson.put("type", action.getType());
            if (action.getTitle() != null) actionJson.put("title", action.getTitle());
            if (action.getMessage() != null) actionJson.put("message", action.getMessage());
            if (action.getPriority() != null) actionJson.put("priority", action.getPriority());
            if (action.getValue() != null) actionJson.put("value", action.getValue());
        } catch (JSONException e) { e.printStackTrace(); }

        return new WorkflowEntity(workflowName, isEnabled, triggerJson.toString(), actionJson.toString());
    }

    public com.example.autoflow.model.Trigger toTrigger() {
        try {
            JSONObject json = new JSONObject(triggerDetails);
            return new com.example.autoflow.model.Trigger(
                    json.getLong("id"),
                    json.getLong("workflowId"),
                    json.getString("type"),
                    json.getString("value")
            );
        } catch (JSONException e) { e.printStackTrace(); return null; }
    }

    public com.example.autoflow.model.Action toAction() {
        try {
            JSONObject json = new JSONObject(actionDetails);
            String type = json.getString("type");
            if (json.has("title") && json.has("message") && json.has("priority")) {
                return new com.example.autoflow.model.Action(type, json.getString("title"), json.getString("message"), json.getString("priority"));
            } else {
                com.example.autoflow.model.Action action = new com.example.autoflow.model.Action(type);
                if (json.has("value")) action.setValue(json.getString("value"));
                return action;
            }
        } catch (JSONException e) { e.printStackTrace(); return null; }
    }
}