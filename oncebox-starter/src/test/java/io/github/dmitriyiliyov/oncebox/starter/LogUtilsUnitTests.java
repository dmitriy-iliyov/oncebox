package io.github.dmitriyiliyov.oncebox.starter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LogUtilsUnitTests {

    @Test
    @DisplayName("UT prettyPrint() should break at braces and commas and indent one tab per nesting level")
    void prettyPrint_shouldBreakAndIndentByNesting() {
        // given
        Object properties = new ToStringOf("Outbox{enabled=true, sender=Sender{type=KAFKA}}");

        // when
        String printed = LogUtils.prettyPrint(properties);

        // then
        assertThat(printed).isEqualTo("""
                Outbox{
                \t enabled=true,
                \t sender=Sender{
                \t\t type=KAFKA
                \t }
                 }""");
    }

    @Test
    @DisplayName("UT prettyPrint() when the text has no braces or commas should return it unchanged")
    void prettyPrint_whenNoStructure_shouldReturnUnchanged() {
        // when / then
        assertThat(LogUtils.prettyPrint(new ToStringOf("plain value"))).isEqualTo("plain value");
    }

    private record ToStringOf(String text) {

        @Override
        public String toString() {
            return text;
        }
    }
}
