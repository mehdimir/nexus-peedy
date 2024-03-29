# NexuSPEEDY

This is Nexus Personal Edition (or Nexus speedy), a "hobby" project of mine, based on Nexus OSS Core, chopped with some not needed features, and added some new ones.  Targeted for "personal" setups, usually on same workstation where developer works.

WWW: https://docs.sonatype.org/display/Nexus/NexusPEEDY

Issue tracking: none yet

This Git repository is mirrored from Sonatype Nexus OSS SVN, and 
here will rebase happen. So, please be careful!

## Main goals:

This is short list of main goals

* speedy, have a "fast forward" of Nexus in development sense, added new features, dropped the bad ones
* speedy, as to be fastest in it's basic function (serving artifacts) of all MRMs
* simple, lightweigt, umm, featherweight deployment and runtime
* hub for experiments with new technologies
* *potentially* serve as source of back-contributes patches to the TRUNK
* ah, and speedy as Gonzales ;)

## Further goals

* have Simpleframework instead of Jetty ;)
* have SimpleXml instead of Modello (and all the "config crap" I did. Mea Culpa for that!)

## Summary of current changes

* Java level raised to 1.6 (instead of 1.5 from trunk)
* Security completely removed (and found a lot of littered security classes in Nexus modules while doing it)
* removed all log4j bound and log4j needing features (and also, lot of littered related classes)
* plexus container/classworlds/utils raised to latest ones
* removed all Jetty dependencies
* removed the nonsense plexus-ehcache usage, ehcache version raised to 1.7.1
* using new (fixed) plexus-slf4j-logger manager, slf4j raised to 1.5.8
* added new modules (Vaadin UI, Jersey REST API using Enunciate, WAR)
* cleaned up dependencies

Just as an example, here are the results of "dependency cleanup": the size of the Nexus OSS WAR: 14.4MB. 
The size of NexusPEEDY WAR: 12.6. Size of "bundle" of Nexus OSS: 15.1MB. The size
of NexusPEEDY "bundle": 13.2MB. *Note*: This comparison is only to show size differences,
but in reality, the build results of nexus-webapp module in NexusPEEDY does not make any 
sense, it is even taken out of reactor build! NexusPEEDY adds new modules for building the
final artifact!

## How to build it

Note: if your local repository contains "official" Nexus snapshot artifacts, due to the [MNG-4368](http://jira.codehaus.org/browse/MNG-4368) issue, you MUST use separate local repository to build this beast!

### Prerequisites

* Java6 and Maven3 ;)

* Your Maven settings set up in same was as is needed for Nexus OSS build.

* Build Plexus Loggers from trunk. They contain a [fix](http://fisheye.codehaus.org/changelog/plexus/?cs=8566) that is not yet released.
  
  Checkout from [Plexus Repository](http://svn.codehaus.org/plexus/archive/plexus-logging/trunk/) and "mvn clean install".

* Build modded Enunciate (although Ryan already incorporated the needed [changes](http://jira.codehaus.org/browse/ENUNCIATE-385) into it's trunk.

  Checkout from [Enunciate GIT](http://github.com/cstamas/enunciate/) and "mvn clean install".
  
* Checkout nexus-peedy and build.

## Branches

I used as manual a nice blog entry to create a "one way" mirror of SVN repository:

http://www.fnokd.com/2008/08/20/mirroring-svn-repository-to-github/

So, the setup is similar, we have the following branches:

* vendor - this is 1:1 mirror of the TRUNK of the Nexus SVN, no changes here.
* master - this is where work is, and *rebase* happens regularly (done manually, not cron-ed like Bob did!)


Have fun!   
~t~
