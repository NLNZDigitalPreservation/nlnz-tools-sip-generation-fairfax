package nz.govt.natlib.tools.sip.generation.fairfax.processor.logging

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.Filter
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.appender.ConsoleAppender
import org.apache.logging.log4j.core.config.Configuration
import org.apache.logging.log4j.core.config.ConfigurationFactory
import org.apache.logging.log4j.core.config.ConfigurationSource
import org.apache.logging.log4j.core.config.Order
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration
import org.apache.logging.log4j.core.config.plugins.Plugin

@Plugin(name = "ProcessorLoggingConfigurationFactory", category = CATEGORY)
@Order(50)
class ProcessorLoggingConfigurationFactory extends ConfigurationFactory {

    // c{precision} or logger{precision} - logger{1} is just class name
    // d{pattern} or date{pattern} - date with %date{DEFAULT_MICROS} as 2012-11-02 14:34:02,123456
    // ex|exception|throwable - exception with stack trace
    // p|level - logging level
    // L|line - line number (does increase overhead)
    // m|msg|message - message
    // n - the platform-dependent line separator
    // t|tn|thread|threadName - name of the thread
    // %-5level means the logging level is left justified to a width of 5 characters
    static final String DEFAULT_PATTERN = "%date{DEFAULT_MICROS} [%threadName] %-5level %logger{1}: %message%n%throwable"
    static final String DEFAULT_PATTERN_WITH_LINE_NUMBERS = "%date{DEFAULT_MICROS} [%threadName] %-5level %logger{1}:%line - %message%n%throwable"

    static Configuration createConfiguration(String name, ConfigurationBuilder<BuiltConfiguration> builder) {
        builder.setConfigurationName(name)
        builder.setStatusLevel(Level.INFO)
        builder.add(builder.newFilter("ThresholdFilter", Filter.Result.ACCEPT, Filter.Result.NEUTRAL)
                .addAttribute("level", Level.INFO))

        AppenderComponentBuilder appenderBuilder = builder.newAppender("stdout", "Console")
                .addAttribute("target", ConsoleAppender.Target.SYSTEM_OUT)

        appenderBuilder.add(builder.newLayout("PatternLayout")
                .addAttribute("pattern", DEFAULT_PATTERN))

        // There must be at least one filter to determine whether a log message is accepted or denied.
        // Use of the MarkerFilter with FLOW means that any flow-related log messages are filtered out,
        // as shown by a match being DENY and a non-match being NEUTRAL.
        // Flow-related log messages are created with log.entry(), log.exit(), log.traceEntry() and log.traceExit()
        // NOTE that it appears that if an appender has more than 1 filter added then there is a console message:
        //     ERROR appender Console has no parameter that matches element ThresholdFilter
        // appenderBuilder.add(builder.newFilter("MarkerFilter", Filter.Result.DENY,
        //        Filter.Result.NEUTRAL).addAttribute("marker", "FLOW"))
        appenderBuilder.add(builder.newFilter("ThresholdFilter", Filter.Result.ACCEPT,
                Filter.Result.NEUTRAL).addAttribute("level", Level.INFO))
        builder.add(appenderBuilder)

        builder.add(builder.newRootLogger(Level.INFO).add(builder.newAppenderRef("stdout")))

        return builder.build()
    }

    @Override
    Configuration getConfiguration(final LoggerContext loggerContext, final ConfigurationSource source) {
        return getConfiguration(loggerContext, source.toString(), null)
    }

    @Override
    Configuration getConfiguration(final LoggerContext loggerContext, final String name, final URI configLocation) {
        ConfigurationBuilder<BuiltConfiguration> builder = newConfigurationBuilder()
        return createConfiguration(name, builder)
    }

    @Override
    protected String[] getSupportedTypes() {
        return [ "*" ]
    }
}
