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

## Branches

I used as manual a nice blog entry to create a "one way" mirror of SVN repository:

http://www.fnokd.com/2008/08/20/mirroring-svn-repository-to-github/

So, the setup is similar, we have the following branches:

* vendor - this is 1:1 mirror of the TRUNK of the Nexus SVN, no changes here.
* master - this is where work is, and *rebase* happens regularly (done manually, not cron-ed like Bob did!)


Have fun!   
~t~
