# CHANGES

The releases are tagged with the date of the release. Currently, there are no version numbers.

## Unreleased

Unreleased changes can be found on the stable_5x and stable_6x branches

- Breaking change: ...
- API fix: ...
- API change: ...
- Fix: ...
- Validation: ...
- Testing improvements: ...

## April 6h, 2017
- API change: Enabled the ScienceDirect complete view.
- API change: Updated the elsevier configuration so multiple DOI metadata fields can be configured.
- API fix: Updated the pubmed import record mapping to be able to map an abstract text that is divided into multiple parts.
- API fix: Updated the pubmed import record mapping to be able to map a DOI from multiple pubmed fields. 
- Fix: The entitlement check is now performed even when the embed page is disabled. The entitlement check can be disabled separately in the configuration.

## March 24th, 2017 (DSpace 5 only)
- API change: Accepted Manuscripts are shown on the embed page if the user is not entitled to the full PDF and the record is not under embargo.
- Fix: Fixed a bug in the bulk import layout for Mirage 1 that caused some checkboxes to be rendered on the right side of the page.

## February 24th, 2017

- API change: Import source URL configuration for Scopus and Pubmed has been moved to config file elsevier-sciencedirect.cfg
- API change: Scopus import source view configuration has been moved to config file elsevier-sciencedirect.cfg
- API change: Scopus abstracts are mapped to DSpace metadata field elsevier.description.scopusabstract
- API fix: According to the Scopus policies the Scopus abstracts are not allowed to be displayed publicly. Scopus abstracts are hidden for all users except DSpace administrators. 

## February 22th, 2017

- API change: Support separating a record value from an import source into multiple DSpace metadata values
- API change: Extra logging in import sources
- Fix: Java class ThemeResourceReader is now included in the patch to prevent errors in DSpace 5.4 to 5.0

## February 3rd, 2017

- API change: Added a batch item import step to select the import source
- API change: Added support for batch item imports from Scopus and PubMed

## January 30th, 2017

- API change: Added a submission step to select the import source
- API change: Added support for item imports from Scopus and PubMed
- API change: Added support for entitlement checks by Scopus ID, PubMed ID or DOI
- API change: Added support for the embedded view of items with a Scopus ID, PubMed ID or DOI
- Fix: The "View publisher version" link on the item page is hidden when the publisher url is not available
- Fix: Updated the encoding of search queries to the import sources to encode spaces correctly

## December 9th, 2016

- API change: The entitlement check now differentiates between "Open Access" and “You have access”
- API change: On theme Mirage the entitlements check now also appears on the full item view
- Fix: The access option selected in the submission upload step is now applied correctly