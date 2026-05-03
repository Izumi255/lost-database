package com.lost.database.dao;

import com.lost.database.entity.DialogueLine;
import com.lost.database.pool.ConnectionPool;
import java.util.List;

public class DialogueLineDao extends GenericDao<DialogueLine, Long> {
    public DialogueLineDao(ConnectionPool pool) {
        super(pool, DialogueLine.class, "dialogue_lines");
    }

    public List<DialogueLine> findByNpcId(Long npcId) {
        return findByField("npc_id", npcId);
    }
}
