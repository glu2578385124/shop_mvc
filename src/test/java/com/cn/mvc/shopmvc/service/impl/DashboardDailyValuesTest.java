package com.cn.mvc.shopmvc.service.impl;

import com.cn.mvc.shopmvc.beans.WeekBean;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class DashboardDailyValuesTest {
    private List<WeekBean> days() {
        return Arrays.asList(new WeekBean("2026-11-25", "Wednesday", 0, 2),
                new WeekBean("2026-11-26", "Thursday", 0, 1),
                new WeekBean("2026-11-27", "Friday", 0, 0));
    }

    @Test
    public void sparseRowsAreAlignedOnceWithoutOverwritingPreviousDays() {
        Map<String, Long> rows = new HashMap<>();
        rows.put("2026-11-25", 3000000000L);
        rows.put("2026-11-27", 214L);
        rows.put("2026-11-20", 999L);
        LinkedHashMap<String, Long> actual = DashboardServiceImpl.alignDailyValues(days(), rows);
        assertEquals(Arrays.asList("Wednesday", "Thursday", "Friday"), new ArrayList<>(actual.keySet()));
        assertEquals(Arrays.asList(3000000000L, 0L, 214L), new ArrayList<>(actual.values()));
    }

    @Test
    public void noRowsStillProducesTheEntireDateAxis() {
        assertEquals(Arrays.asList(0L, 0L, 0L), new ArrayList<>(
                DashboardServiceImpl.alignDailyValues(days(), Collections.<String, Long>emptyMap()).values()));
    }
}
