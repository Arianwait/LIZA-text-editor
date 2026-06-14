package kz.aws.gametexteditor.model;

import java.io.Serializable;

public class EditorCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    private String type;
    private String action;
    private String target;
    private String value;
    private String id;
    private String flag;
    private String key;
    private String prompt;
    private int onSuccess = -1;
    private int onFailure = -1;

    // Visual effect fields
    private String effect;
    private int duration = -1;
    private double startScale = -1;
    private double endScale = -1;
    private String filter;
    private double intensity = -1;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFlag() { return flag; }
    public void setFlag(String flag) { this.flag = flag; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public int getOnSuccess() { return onSuccess; }
    public void setOnSuccess(int onSuccess) { this.onSuccess = onSuccess; }

    public int getOnFailure() { return onFailure; }
    public void setOnFailure(int onFailure) { this.onFailure = onFailure; }

    public String getEffect() { return effect; }
    public void setEffect(String effect) { this.effect = effect; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public double getStartScale() { return startScale; }
    public void setStartScale(double startScale) { this.startScale = startScale; }

    public double getEndScale() { return endScale; }
    public void setEndScale(double endScale) { this.endScale = endScale; }

    public String getFilter() { return filter; }
    public void setFilter(String filter) { this.filter = filter; }

    public double getIntensity() { return intensity; }
    public void setIntensity(double intensity) { this.intensity = intensity; }
}
