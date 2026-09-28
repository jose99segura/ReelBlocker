package app.reelblocker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileLogicTest {

    @Test
    fun `ranks change every five levels and cap at legend`() {
        assertEquals(Profile.Rank.ROOKIE, Profile.rankForLevel(1))
        assertEquals(Profile.Rank.ROOKIE, Profile.rankForLevel(4))
        assertEquals(Profile.Rank.APPRENTICE, Profile.rankForLevel(5))
        assertEquals(Profile.Rank.GUARDIAN, Profile.rankForLevel(10))
        assertEquals(Profile.Rank.LEGEND, Profile.rankForLevel(30))
        assertEquals(Profile.Rank.LEGEND, Profile.rankForLevel(99))
    }

    @Test
    fun `rank minLevel matches rankForLevel`() {
        for (rank in Profile.Rank.entries) {
            assertEquals(rank, Profile.rankForLevel(rank.minLevel))
        }
        assertNull(Profile.Rank.LEGEND.next)
    }

    @Test
    fun `only crossing a multiple of five is a rank up`() {
        assertFalse(Profile.LevelUp(7, 8).isRankUp)
        assertTrue(Profile.LevelUp(9, 10).isRankUp)
        assertTrue(Profile.LevelUp(8, 11).isRankUp)
        assertFalse(Profile.LevelUp(10, 14).isRankUp)
    }

    @Test
    fun `block xp is capped per day`() {
        assertTrue(Profile.blockEarnsXp(1))
        assertTrue(Profile.blockEarnsXp(Profile.MAX_XP_BLOCKS_PER_DAY))
        assertFalse(Profile.blockEarnsXp(Profile.MAX_XP_BLOCKS_PER_DAY + 1))
        assertFalse(Profile.blockEarnsXp(0))
    }

    @Test
    fun `graduation xp grows with stars up to the max`() {
        assertEquals(200, Profile.graduationXp(1))
        assertEquals(250, Profile.graduationXp(2))
        assertEquals(400, Profile.graduationXp(Collection.MAX_STARS))
        assertEquals(400, Profile.graduationXp(Collection.MAX_STARS + 3))
    }
}
