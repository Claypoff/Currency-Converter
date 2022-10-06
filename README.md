# Currency Converter

Currency Converter programming exercise for the ResNexus interview

## Installation

This program is a maven project and depends on using the gson and sqlite-jdbc libraries. 

Open the **pom.xml** file and sync with maven to download these dependencies packages to run the program.

## Usage

This program offers 4 options:

1. Converting USD amount to any currency
    * This will ask the user to give a valid currency code and use a datatable to convert the currency
    
2. Converting any currency amount to another currency
    * This will ask the user for two valid currency codes and uses an api to make the conversion

3. Printing out all the valid symbols
    * This will print out all the currency codes along with a description 

4. Quit
    * This will close the database connection and end the program
