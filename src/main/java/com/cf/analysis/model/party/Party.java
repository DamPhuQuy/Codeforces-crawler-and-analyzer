package com.cf.analysis.model.party;

import java.lang.reflect.Member;

public class Party {
    private Integer id;
    private Integer contestId = 0;
    private Member[] members = new Member[0];
    private ParticipantType participantType = ParticipantType.CONTESTANT;
    private Integer teamId = 0;
    private String teamName = "";
    private Boolean ghost = false;
    private Integer room = 0;
    private Integer startTimeSeconds = 0;

    public Party() {}

    public Party(Integer contestId, Member[] members, ParticipantType participantType, Integer teamId, String teamName, Boolean ghost, Integer room, Integer startTimeSeconds) {
        this.contestId = contestId;
        this.members = members;
        this.participantType = participantType;
        this.teamId = teamId;
        this.teamName = teamName;
        this.ghost = ghost;
        this.room = room;
        this.startTimeSeconds = startTimeSeconds;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getContestId() {
        return contestId;
    }

    public void setContestId(Integer contestId) {
        this.contestId = contestId;
    }

    public Member[] getMembers() {
        return members;
    }

    public void setMembers(Member[] members) {
        this.members = members;
    }

    public ParticipantType getParticipantType() {
        return participantType;
    }

    public void setParticipantType(ParticipantType participantType) {
        this.participantType = participantType;
    }

    public Integer getTeamId() {
        return teamId;
    }

    public void setTeamId(Integer teamId) {
        this.teamId = teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public Boolean getGhost() {
        return ghost;
    }

    public void setGhost(Boolean ghost) {
        this.ghost = ghost;
    }

    public Integer getRoom() {
        return room;
    }

    public void setRoom(Integer room) {
        this.room = room;
    }

    public Integer getStartTimeSeconds() {
        return startTimeSeconds;
    }

    public void setStartTimeSeconds(Integer startTimeSeconds) {
        this.startTimeSeconds = startTimeSeconds;
    }


}
