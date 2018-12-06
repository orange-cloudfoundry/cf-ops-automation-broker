
https://cumulative-hypotheses.org/2011/08/30/tdd-as-if-you-meant-it/

The Rules

    1-Write exactly one new test, the smallest test you can that seems to point in the direction of a solution
    2-See it fail
    3-Make the test from (1) pass by writing the least implementation code you can in the test method. 
    4-Refactor to remove duplication, and otherwise as required to improve the design. Be strict about using these moves:
        you want a new method—wait until refactoring time, then… create new (non-test) methods by doing one of these, and in no other way:
            preferred: do Extract Method on implementation code created as per (3) to create a new method in the test class, or
            if you must: move implementation code as per (3) into an existing implementation method
        you want a new class—wait until refactoring time, then… create non-test classes to provide a destination for a Move Method and for no other reason
            populate implementation classes with methods by doing Move Method, and no other way


http://blog.adrianbolboaca.ro/2017/06/evolutionary-design-normalize-growth/

http://blog.adrianbolboaca.ro/2017/08/tdd-as-if-you-meant-it-think-red-green-refactor-episode-1/

    Guideline 1: Always start with outputs when doing an analysis
    Guideline 2: Behavior Slicing
    Guideline 3: SIMPLIFY!
    Guideline 4: Introduce only one notion (domain concept) at a time, one per test
    Guideline 5: The rule of three “only extract duplication when spotted at least three times”
    Guideline 6: Triangulation

=> Action: Watch again episode 1 to learn more about behavior slicing

Refactor builder with default value focus tests more

Having this type of builder also lets me have focused tests. I don’t want my test that needs only Age to contain Name. So I want to be able to have playerBuilder.withAge(34).build(). In this case all the values for all the other fields of Player will have a default value that is not generating any side-effects in the system. We could call this characteristic an idempotence of the builder.


Only extract to class when 3 or more members

cleanup and use [automated] code standards, helps discover duplication and removing it

http://blog.adrianbolboaca.ro/2017/10/tdd-as-if-you-meant-it-refactor-and-clean-up-episode-11/


for each desired change, make the change easy (warning: this may be hard), then make the easy change

https://martinfowler.com/articles/preparatory-refactoring-example.html


Action: watch http://blog.adrianbolboaca.ro/2017/12/tdd-as-if-you-meant-it-remote-live-coding-with-grenoble-episode-15/