package com.lost.database.dao;

import static org.junit.jupiter.api.Assertions.*;

import com.lost.database.entity.DialogueLine;
import com.lost.database.entity.Npc;
import java.util.List;
import org.junit.jupiter.api.*;

/** Інтеграційні тести для NpcDao та DialogueLineDao (таблиці npcs, dialogue_lines). */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NpcDaoTest extends BaseDaoTest {

    private static NpcDao npcDao;
    private static DialogueLineDao dialogueLineDao;
    private static Long npcId;

    @BeforeAll
    static void setUp() {
        npcDao = new NpcDao(pool);
        dialogueLineDao = new DialogueLineDao(pool);
    }

    @Test
    @Order(1)
    @DisplayName("save() — збереження нового NPC")
    void testSaveNpc() {
        Npc npc = new Npc();
        npc.setNpcName("Тестовий Герлі");
        npc.setPortraitPath("/portraits/hurley.png");
        npc.setSpritePath("/sprites/hurley.png");
        npc.setLevelNumber(1);
        npc.setSpawnX(100.0);
        npc.setSpawnY(200.0);
        npc.setNpcType("FRIENDLY");

        npcDao.save(npc);

        assertNotNull(npc.getId());
        npcId = npc.getId();
    }

    @Test
    @Order(2)
    @DisplayName("findById() — пошук NPC за ID")
    void testFindNpcById() {
        var found = npcDao.findById(npcId);

        assertTrue(found.isPresent());
        assertEquals("Тестовий Герлі", found.get().getNpcName());
        assertEquals("FRIENDLY", found.get().getNpcType());
    }

    @Test
    @Order(3)
    @DisplayName("findByLevelNumber() — пошук NPC за рівнем")
    void testFindByLevelNumber() {
        // Додамо ще одного NPC на рівень 1
        Npc npc2 = new Npc();
        npc2.setNpcName("Тестовий Бен");
        npc2.setLevelNumber(1);
        npc2.setSpawnX(300.0);
        npc2.setSpawnY(400.0);
        npc2.setNpcType("HOSTILE");
        npcDao.save(npc2);

        List<Npc> npcsOnLevel1 = npcDao.findByLevelNumber(1);

        assertTrue(npcsOnLevel1.size() >= 2);
    }

    @Test
    @Order(4)
    @DisplayName("DialogueLineDao.save() — збереження репліки діалогу")
    void testSaveDialogueLine() {
        DialogueLine line = new DialogueLine();
        line.setNpcId(npcId);
        line.setLineOrder(1);
        line.setSpeakerName("Герлі");
        line.setPortraitKey("hurley_happy");
        line.setDialogueText("Привіт! Ти новенький на острові?");

        dialogueLineDao.save(line);

        assertNotNull(line.getId());
    }

    @Test
    @Order(5)
    @DisplayName("DialogueLineDao.findByNpcId() — пошук реплік NPC")
    void testFindDialoguesByNpcId() {
        // Додамо другу репліку
        DialogueLine line2 = new DialogueLine();
        line2.setNpcId(npcId);
        line2.setLineOrder(2);
        line2.setSpeakerName("Герлі");
        line2.setDialogueText("Тут небезпечно, будь обережним!");
        dialogueLineDao.save(line2);

        List<DialogueLine> lines = dialogueLineDao.findByNpcId(npcId);

        assertTrue(lines.size() >= 2, "NPC повинен мати щонайменше 2 репліки");
    }

    @Test
    @Order(6)
    @DisplayName("deleteById() — видалення NPC (каскадне видалення діалогів)")
    void testDeleteNpc() {
        npcDao.deleteById(npcId);

        assertFalse(npcDao.findById(npcId).isPresent());
        // Каскадне видалення — діалоги теж мають зникнути
        assertTrue(dialogueLineDao.findByNpcId(npcId).isEmpty());
    }
}
