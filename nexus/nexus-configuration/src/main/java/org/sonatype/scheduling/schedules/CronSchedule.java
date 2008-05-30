package org.sonatype.scheduling.schedules;

import org.sonatype.scheduling.iterators.CronIterator;
import org.sonatype.scheduling.iterators.SchedulerIterator;

public class CronSchedule
    extends AbstractSchedule
{
    private final String cronExpression;

    public CronSchedule( String cronExpression )
    {
        super( null, null );

        this.cronExpression = cronExpression;
    }

    public String getCronExpression()
    {
        return cronExpression;
    }

    public SchedulerIterator getIterator()
    {
        return new CronIterator( cronExpression );
    }

}
