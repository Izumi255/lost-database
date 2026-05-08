package com.lost.database.service;

import com.lost.database.dao.DialogueLineDao;
import com.lost.database.dao.NpcDao;
import com.lost.database.entity.DialogueLine;
import com.lost.database.entity.Npc;
import com.lost.database.pool.ConnectionPool;
import java.util.List;
import java.util.Optional;

/**
 * Сервіс бізнес-логіки для NPC та діалогів.
 *
 * <p>Управління неігровими персонажами та їх діалоговими лініями.
 */
public class NpcService {

    private final NpcDao npcDao;
    private final DialogueLineDao dialogueLineDao;

    public NpcService(ConnectionPool pool) {
        this.npcDao = new NpcDao(pool);
        this.dialogueLineDao = new DialogueLineDao(pool);
    }

    /** Отримати всіх NPC. */
    public List<Npc> findAllNpcs() {
        return npcDao.findAll();
    }

    /** Знайти NPC за ID. */
    public Optional<Npc> findNpcById(Long id) {
        return npcDao.findById(id);
    }

    /** Отримати діалогові лінії для конкретного NPC. */
    public List<DialogueLine> getDialoguesForNpc(Long npcId) {
        return dialogueLineDao.findByNpcId(npcId);
    }

    /** Отримати всі діалогові лінії. */
    public List<DialogueLine> findAllDialogues() {
        return dialogueLineDao.findAll();
    }

    /** Додати нову діалогову лінію для NPC. */
    public void addDialogueLine(Long npcId, int lineOrder, String text) {
        DialogueLine line = new DialogueLine();
        line.setNpcId(npcId);
        line.setLineOrder(lineOrder);
        line.setDialogueText(text);
        dialogueLineDao.save(line);
    }
}
