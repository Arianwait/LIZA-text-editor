package kz.aws.gametexteditor.model;

import java.io.Serializable;

public class EditorChoice implements Serializable {

    private static final long serialVersionUID = 1L;

    private String text;
    private int requestsId;
    private String id;
    private String description;
    private String addChoice = "";
    private String removeChoice = "";
    private String characterChoice = "";
    private int minRep = 0;
    private int maxRep = 100;

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public int getRequestsId() { return requestsId; }
    public void setRequestsId(int requestsId) { this.requestsId = requestsId; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAddChoice() { return addChoice; }
    public void setAddChoice(String addChoice) { this.addChoice = addChoice; }

    public String getRemoveChoice() { return removeChoice; }
    public void setRemoveChoice(String removeChoice) { this.removeChoice = removeChoice; }

    public String getCharacterChoice() { return characterChoice; }
    public void setCharacterChoice(String characterChoice) { this.characterChoice = characterChoice; }

    public int getMinRep() { return minRep; }
    public void setMinRep(int minRep) { this.minRep = minRep; }

    public int getMaxRep() { return maxRep; }
    public void setMaxRep(int maxRep) { this.maxRep = maxRep; }
}
