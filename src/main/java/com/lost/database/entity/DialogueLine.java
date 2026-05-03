package com.lost.database.entity;

/** Сутність репліки діалогу. */
public class DialogueLine {

    private Long id;
    private Long npcId;
    private int lineOrder;
    private String speakerName;
    private String portraitKey;
    private String dialogueText;
    private String triggerCondition;

    public DialogueLine() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNpcId() {
        return npcId;
    }

    public void setNpcId(Long npcId) {
        this.npcId = npcId;
    }

    public int getLineOrder() {
        return lineOrder;
    }

    public void setLineOrder(int lineOrder) {
        this.lineOrder = lineOrder;
    }

    public String getSpeakerName() {
        return speakerName;
    }

    public void setSpeakerName(String speakerName) {
        this.speakerName = speakerName;
    }

    public String getPortraitKey() {
        return portraitKey;
    }

    public void setPortraitKey(String portraitKey) {
        this.portraitKey = portraitKey;
    }

    public String getDialogueText() {
        return dialogueText;
    }

    public void setDialogueText(String dialogueText) {
        this.dialogueText = dialogueText;
    }

    public String getTriggerCondition() {
        return triggerCondition;
    }

    public void setTriggerCondition(String triggerCondition) {
        this.triggerCondition = triggerCondition;
    }

    @Override
    public String toString() {
        return "DialogueLine{id=" + id + ", speaker='" + speakerName + "'}";
    }
}
