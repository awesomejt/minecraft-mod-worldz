package media.jlt.minecraft.mods.worldz.config.schema;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Exercises {@link Setting#group} and {@link SchemaSection#copyInto} in isolation (DESIGN §42.2;
 * TODO 25.6a) against a synthetic two-field POJO, since no real {@code *Schema.java} nests a group
 * yet -- 25.6b is the first sub-step to do that against a real section. Guards the one subtlety the
 * mechanism has: {@link Setting#group}'s setter must copy the freshly-parsed group value's fields
 * <em>onto</em> the real target, not the other way around, which a bare {@code group::copyInto}
 * method reference would get backwards given {@link Accessor#set}'s {@code (owner, value)}
 * argument order.
 */
class SettingGroupTest {
    private static final Logger LOGGER = NOPLogger.NOP_LOGGER;
    private static final RootSchema ROOT = new RootSchema();

    @Test
    void readCopiesTheGroupsFieldsOntoTheRealTargetNotTheReverse() {
        Map<String, Object> subMap = new LinkedHashMap<>();
        subMap.put("inner", 7);
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("outer", 5);
        raw.put("sub", subMap);

        Owner parsed = ROOT.read(raw, new ParseContext(LOGGER));

        assertEquals(5, parsed.outer);
        assertEquals(7, parsed.inner);
    }

    @Test
    void sanitizeIsASelfAssignNoOpThatPreservesBothFields() {
        Owner owner = new Owner();
        owner.outer = 3;
        owner.inner = 9;

        Owner sanitized = ROOT.sanitize(owner, new SanitizeContext(LOGGER, null));

        assertEquals(3, sanitized.outer);
        assertEquals(9, sanitized.inner);
    }

    @Test
    void toMapEmitsTheGroupAsANestedSubMap() {
        Owner owner = new Owner();
        owner.outer = 1;
        owner.inner = 2;

        Map<String, Object> map = ROOT.toMap(owner);

        assertEquals(1, map.get("outer"));
        assertFalse(map.get("sub") instanceof Owner, "the group must emit a sub-map, not the raw owner");
        @SuppressWarnings("unchecked")
        Map<String, Object> subMap = (Map<String, Object>) map.get("sub");
        assertEquals(2, subMap.get("inner"));
    }

    /** Synthetic stand-in for a real config POJO: one field the root owns directly, one that lives
     * inside a group. */
    static final class Owner {
        int outer;
        int inner;
    }

    /** The group: declares only {@code inner}, at dotted path {@code "sub.inner"}. */
    static final class SubGroup extends SchemaSection<Owner> {
        SubGroup() {
            super("sub", Owner::new);
        }

        @Override
        protected List<Setting<Owner, ?>> declare() {
            return List.of(
                Setting.<Owner>integer("inner", o -> o.inner, (o, v) -> o.inner = v).doc("test").build()
            );
        }
    }

    static final class RootSchema extends SchemaSection<Owner> {
        private final SubGroup sub = new SubGroup();

        RootSchema() {
            super("", Owner::new);
        }

        @Override
        protected List<Setting<Owner, ?>> declare() {
            return List.of(
                Setting.<Owner>integer("outer", o -> o.outer, (o, v) -> o.outer = v).doc("test").build(),
                Setting.group("sub", sub).doc("test").build()
            );
        }
    }
}
