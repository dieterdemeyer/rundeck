import java.util.logging.LogRecord
import java.text.SimpleDateFormat
import grails.test.GrailsUnitTestCase

class ExecutionServiceTests extends GrailsUnitTestCase {

    void testParseLogDetail() {
        def s1 = "[greg@localhost Test.shellutil whoami][WARN] some more text"
        def t1 = HtTableLogger.parseLogDetail(s1)
        assert t1.size() > 0
        assertEquals "greg", t1.user
        assertEquals "localhost", t1.node
        assertEquals "Test.shellutil", t1.context
        assertEquals "whoami", t1.command
        assertEquals "WARN", t1.level
        assertEquals "some more text", t1.rest

        def s2 = "[greg@localhost Test.shellutil.aName whoami][WARN] some more text"
        def t2 = HtTableLogger.parseLogDetail(s2)
        assert t2.size() > 0
        assertEquals "user incorrect: ${t2.user}","greg", t2.user
        assertEquals "node incorrect: ${t2.node}","localhost", t2.node
        assertEquals "context incorrect: ${t2.context}","Test.shellutil.aName", t2.context
        assertEquals "command incorrect: ${t2.command}","whoami", t2.command
        assertEquals "level incorrect: ${t2.level}","WARN", t2.level
        assertEquals "rest incorrect: ${t2.rest}","some more text", t2.rest

        def s3 = "[greg@Gozer.local Test.shellutil ProduceSomething][INFO] some more text"
        def t3 = HtTableLogger.parseLogDetail(s3)
        assert t3.size() > 0
        assertEquals "greg", t3.user
        assertEquals "Gozer.local", t3.node
        assertEquals "Test.shellutil", t3.context
        assertEquals "ProduceSomething", t3.command
        assertEquals "INFO", t3.level
        assertEquals "some more text", t3.rest

        //allow simplified format [user@host][LEVEL] msg
        def s4 = "[greg@Gozer.local][WARN] some more text"
        def t4 = HtTableLogger.parseLogDetail(s4)
        assert t4.size() > 0
        assertEquals "greg", t4.user
        assertEquals "Gozer.local", t4.node
        assertNull    "context should be null",t4.context
        assertNull    "command should be null",t4.command
        assertEquals "WARN", t4.level
        assertEquals "some more text", t4.rest


        //allow simplified format [user@host][LEVEL] msg
        def s5 = "[greg@rundeck@Gozer.local][WARN] some more text"
        def t5 = HtTableLogger.parseLogDetail(s5)
        assert t5.size() > 0
        assertEquals "greg", t5.user
        assertEquals "rundeck@Gozer.local", t5.node
        assertNull    "context should be null",t5.context
        assertNull    "command should be null",t5.command
        assertEquals "WARN", t5.level
        assertEquals "some more text", t5.rest


        //test run format [user@host command][LEVEL] msg
        def s6 = "[greg@rundeck@Gozer.local run][WARN] some more text"
        def t6 = HtTableLogger.parseLogDetail(s6)
        assert t6.size() > 0
        assertEquals "greg", t6.user
        assertEquals "rundeck@Gozer.local", t6.node
        assertNull    "context should be null",t6.context
        assertEquals "run",t6.command
        assertEquals "WARN", t6.level
        assertEquals "some more text", t6.rest


        //*****
        //test incorrect format in various ways...
        //*****
        //missing brackets
        t:{
            def text = "[greg@Gozer.local Test.shellutil ProduceSomething[INFO] some more text"
            def map = HtTableLogger.parseLogDetail(text)
            assertEquals 0, map.size()
        };
        t:{
            def text = "[greg@Gozer.local Test.shellutil ProduceSomething]INFO] some more text"
            def map = HtTableLogger.parseLogDetail(text)
            assertEquals 0, map.size()
        };
        t:{
            def text = "[greg@Gozer.local Test.shellutil ProduceSomething][INFO some more text"
            def map = HtTableLogger.parseLogDetail(text)
            assertEquals 0, map.size()
        };
        t:{
            def text = "greg@Gozer.local Test.shellutil ProduceSomething][INFO] some more text"
            def map = HtTableLogger.parseLogDetail(text)
            assertEquals 0, map.size()
        };
        //missing @ symbol in user@node
        t:{
            def text = "[greg.Gozer.local Test.shellutil ProduceSomething][INFO] some more text"
            def map = HtTableLogger.parseLogDetail(text)
            assertEquals 2, map.size()
        };

        //single space indicates simple command name
        t:{
            def text = "[greg@Gozer.localTest.shellutil ProduceSomething][INFO] some more text"
            def map = HtTableLogger.parseLogDetail(text)
            assertEquals 5, map.size()
        };
        t:{
            def text = "[greg@Gozer.local Test.shellutilProduceSomething][INFO] some more text"
            def map = HtTableLogger.parseLogDetail(text)
            assertEquals 5, map.size()
        };
        //empty loglevel
        t:{
            def text = "[greg@Gozer.local Test.shellutil ProduceSomething][] some more text"
            def map = HtTableLogger.parseLogDetail(text)
            assertEquals 0, map.size()
        };
        //single space is simple command
        t:{
            def text = "[greg@Gozer.local Test.shellutil][WARN] some more text"
            def map = HtTableLogger.parseLogDetail(text)
            assertEquals 5, map.size()
        };
        t:{
            def text = "[greg@Gozer.local][] some more text"
            def map = HtTableLogger.parseLogDetail(text)
            assertEquals 0, map.size()
        };
        t:{
            def text = "[][WARN] some more text"
            def map = HtTableLogger.parseLogDetail(text)
            assertEquals 0, map.size()
        };
        t:{
            def text = "[][] some more text"
            def map = HtTableLogger.parseLogDetail(text)
            assertEquals 0, map.size()
        };
        t:{
            def text = "[] some more text"
            def map = HtTableLogger.parseLogDetail(text)
            assertEquals 0, map.size()
        };
        t:{
            def text = "some more text"
            def map = HtTableLogger.parseLogDetail(text)
            assertEquals 0, map.size()
        };
        //missing level section
        t:{
            def text = "[greg@Gozer.local Test.shellutil ProduceSomething] some more text"
            def map = HtTableLogger.parseLogDetail(text)
            assertEquals 0, map.size()
        };
        t:{
            def text = "[greg@Gozer.local Test.shellutil] some more text"
            def map = HtTableLogger.parseLogDetail(text)
            assertEquals 0, map.size()
        };
        t:{
            def text = "[greg@Gozer.local] some more text"
            def map = HtTableLogger.parseLogDetail(text)
            assertEquals 0, map.size()
        };

    }


    void testHTFormatter(){
        def SimpleDateFormat fmt = new SimpleDateFormat("hh:mm:ss")
        def HtFormatter hf = new HtFormatter()
        assertNotNull hf

        //
        def LogRecord lr = new LogRecord(java.util.logging.Level.SEVERE,"This is a test")
        def String s =hf.format(lr)
        def tstring = fmt.format(new Date(lr.getMillis()))
        assertEquals "formatting was incorrect: ${s}","^^^${tstring}|SEVERE|This is a test^^^",s

        //add extra \r char
        lr = new LogRecord(java.util.logging.Level.SEVERE,"This is a test\r")
        s =hf.format(lr)
        tstring = fmt.format(new Date(lr.getMillis()))
        assertEquals "formatting was incorrect: ${s}","^^^${tstring}|SEVERE|This is a test^^^",s

        //add metadata
        def map = [user:'user1',module:'AModule',command:'aCmd',node:'someNode',context:'Proj.AModule.something']
        s =hf.format(lr,map)
        tstring = fmt.format(new Date(lr.getMillis()))
        assertEquals "formatting was incorrect: ${s}","^^^${tstring}|SEVERE|user1|AModule|aCmd|someNode|Proj.AModule.something|This is a test^^^",s

        //define some blank metadata
        map = [user:'',module:'AModule',command:'aCmd',node:'',context:'Proj.AModule.something']
        s =hf.format(lr,map)
        tstring = fmt.format(new Date(lr.getMillis()))
        assertEquals "formatting was incorrect: ${s}","^^^${tstring}|SEVERE||AModule|aCmd||Proj.AModule.something|This is a test^^^",s


        //define some null metadata
        map = [/* user:'',*/ module:'AModule',command:'aCmd',/* node:'', */ context:'Proj.AModule.something']
        s =hf.format(lr,map)
        tstring = fmt.format(new Date(lr.getMillis()))
        assertEquals "formatting was incorrect: ${s}","^^^${tstring}|SEVERE||AModule|aCmd||Proj.AModule.something|This is a test^^^",s

        //define all null metadata
        map = [:]
        s =hf.format(lr,map)
        tstring = fmt.format(new Date(lr.getMillis()))
        assertEquals "formatting was incorrect: ${s}","^^^${tstring}|SEVERE||||||This is a test^^^",s


    }

    void testValidateInputOptionValues(){
		mockDomain(ScheduledExecution)
		mockDomain(Option)
        ScheduledExecution se = new ScheduledExecution()
		def testService = new ExecutionService()
		def frameworkService = new FrameworkService()
        testService.frameworkService=frameworkService

        t:{
            //test regex and optional value
            assertTrue testService.validateInputOptionValues(se,[:])
            ScheduledExecution se2 = new ScheduledExecution()
            se2.addToOptions(new Option(name:'test1',enforced:false))
            se2.addToOptions(new Option(name:'test2',enforced:false,regex:'.*abc.*'))
            assertNotNull(se2.options)
            assertEquals(2,se2.options.size())

            assertTrue testService.validateInputOptionValues(se2,['option.test1':'some value'])
            assertTrue testService.validateInputOptionValues(se2,[argString:'-test1 \'some value\''])
            assertTrue testService.validateInputOptionValues(se2,['option.test1':'some value','option.test2':'abc'])
            assertTrue testService.validateInputOptionValues(se2,[argString:'-test1 \'some value\' -test2 abc'])
            assertTrue testService.validateInputOptionValues(se2,['option.test1':'some value','option.test2':'abcdefg'])
            assertTrue testService.validateInputOptionValues(se2,[argString:'-test1 \'some value\' -test2 abcdefg'])
            assertTrue testService.validateInputOptionValues(se2,['option.test1':'some value','option.test2':'xyzabcdefg'])
            assertTrue testService.validateInputOptionValues(se2,[argString:'-test1 \'some value\' -test2 xyzabcdefg'])
            try{
                testService.validateInputOptionValues(se2,['option.test1':'some value','option.test2':'xyz'])
                fail("Should have thrown exception")
            }catch (Exception e){
                assertNotNull(e)
                assertTrue( e.message.contains("'test2' doesn't match regular expression"))
            }
            try{
                testService.validateInputOptionValues(se2,[argString:'-test1 \'some value\' -test2 xyz'])
                fail("Should have thrown exception")
            }catch (Exception e){
                assertNotNull(e)
                assertTrue( e.message.contains("'test2' doesn't match regular expression"))
            }
            try{
                testService.validateInputOptionValues(se2,['option.test1':'some value','option.test2':'xyzab'])
                fail("Should have thrown exception")
            }catch (Exception e){
                assertNotNull(e)
                assertTrue( e.message.contains("'test2' doesn't match regular expression"))
            }
            try{
                testService.validateInputOptionValues(se2,[argString:'-test1 \'some value\' -test2 xyzab'])
                fail("Should have thrown exception")
            }catch (Exception e){
                assertNotNull(e)
                assertTrue( e.message.contains("'test2' doesn't match regular expression"))
            }
            se2.addToOptions(new Option(name:'test3',enforced:false,regex:'shampoo[abc].*'))

            assertTrue testService.validateInputOptionValues(se2,['option.test3':'shampooa'])
            assertTrue testService.validateInputOptionValues(se2,['option.test3':'shampoob'])
            assertTrue testService.validateInputOptionValues(se2,['option.test3':'shampooc'])
            assertTrue testService.validateInputOptionValues(se2,['option.test3':'shampoocxyz234'])
            assertTrue testService.validateInputOptionValues(se2,[argString:'-test3 shampooa'])
            assertTrue testService.validateInputOptionValues(se2,[argString:'-test3 shampoob'])
            assertTrue testService.validateInputOptionValues(se2,[argString:'-test3 shampooc'])
            assertTrue testService.validateInputOptionValues(se2,[argString:'-test3 shampoocxyz234'])

            try{
                testService.validateInputOptionValues(se2,['option.test3':'shampooz'])
                fail("Should have thrown exception")
            }catch (Exception e){
                assertNotNull(e)
                assertTrue( e.message.contains("'test3' doesn't match regular expression"))
            }
            try{
                testService.validateInputOptionValues(se2,[argString:'-test3 shampooz'])
                fail("Should have thrown exception")
            }catch (Exception e){
                assertNotNull(e)
                assertTrue( e.message.contains("'test3' doesn't match regular expression"))
            }
            try{
                testService.validateInputOptionValues(se2,['option.test3':'zshampooa'])
                fail("Should have thrown exception")
            }catch (Exception e){
                assertNotNull(e)
                assertTrue( e.message.contains("'test3' doesn't match regular expression"))
            }
            try{
                testService.validateInputOptionValues(se2,[argString:'-test3 zshampooa'])
                fail("Should have thrown exception")
            }catch (Exception e){
                assertNotNull(e)
                assertTrue( e.message.contains("'test3' doesn't match regular expression"))
            }
        }

        t:{
            //test enforced values list
            assertTrue testService.validateInputOptionValues(se,[:])
            ScheduledExecution se2 = new ScheduledExecution()
            final Option option = new Option(name: 'test1', enforced: true)
            option.addToValues('a')
            option.addToValues('b')
            option.addToValues('abc')
            se2.addToOptions(option)
            assertNotNull(se2.options)
            assertEquals(1,se2.options.size())

            assertTrue testService.validateInputOptionValues(se2,['option.test1':'a'])
            assertTrue testService.validateInputOptionValues(se2,['option.test1':'b'])
            assertTrue testService.validateInputOptionValues(se2,['option.test1':'abc'])
            assertTrue testService.validateInputOptionValues(se2,[argString:'-test1 a'])
            assertTrue testService.validateInputOptionValues(se2,[argString:'-test1 b'])
            assertTrue testService.validateInputOptionValues(se2,[argString:'-test1 abc'])
            try{
                testService.validateInputOptionValues(se2,['option.test1':'some value'])
                fail("Should have thrown exception")
            }catch (Exception e){
                assertNotNull(e)
                assertTrue( e.message.contains("Option 'test1'"))
                assertTrue( e.message.contains("was not in the allowed values"))
            }
            try{
                testService.validateInputOptionValues(se2,[argString:'-test1 \'some value\''])
                fail("Should have thrown exception")
            }catch (Exception e){
                assertNotNull(e)
                assertTrue( e.message.contains("Option 'test1'"))
                assertTrue( e.message.contains("was not in the allowed values"))
            }
            try{
                testService.validateInputOptionValues(se2,['option.test1':'abd'])
                fail("Should have thrown exception")
            }catch (Exception e){
                assertNotNull(e)
                assertTrue( e.message.contains("Option 'test1'"))
                assertTrue( e.message.contains("was not in the allowed values"))
            }
            try{
                testService.validateInputOptionValues(se2,[argString:'-test1 abd'])
                fail("Should have thrown exception")
            }catch (Exception e){
                assertNotNull(e)
                assertTrue( e.message.contains("Option 'test1'"))
                assertTrue( e.message.contains("was not in the allowed values"))
            }

            //test1 is not required, so value can be absent:
            assertTrue testService.validateInputOptionValues(se2,[:])
            assertTrue testService.validateInputOptionValues(se2,[argString:''])

        }


        t:{
            //test required & values list
            assertTrue testService.validateInputOptionValues(se,[:])
            ScheduledExecution se2 = new ScheduledExecution()
            final Option option = new Option(name: 'test1', enforced: true, required:true)
            option.addToValues('a')
            option.addToValues('b')
            option.addToValues('abc')
            se2.addToOptions(option)
            assertNotNull(se2.options)
            assertEquals(1,se2.options.size())
            //test1 is required, so value cannot be absent:
            try{
                testService.validateInputOptionValues(se2,[:])
                fail("Should have thrown exception")
            }catch (Exception e){
                assertNotNull(e)
                assertTrue( e.message.contains("'test1' is required"))
            }
            try{
                testService.validateInputOptionValues(se2,[argString:''])
                fail("Should have thrown exception")
            }catch (Exception e){
                assertNotNull(e)
                assertTrue( e.message.contains("'test1' is required"))
            }
        }

        t:{
            //test non-multi-valued and list input
            assertTrue testService.validateInputOptionValues(se,[:])
            ScheduledExecution se2 = new ScheduledExecution()
            final Option option = new Option(name: 'test1', required:false, enforced: true, multivalued: false)
            option.addToValues('a')
            option.addToValues('b')
            option.addToValues('abc')
            se2.addToOptions(option)
            assertNotNull(se2.options)
            assertEquals(1,se2.options.size())
            //valid single value input
            assertTrue testService.validateInputOptionValues(se2, ['option.test1': 'abc'])
            try{
                //should fail with list input
                testService.validateInputOptionValues(se2,['option.test1':['blah']])
                fail("Should have thrown exception")
            }catch (Exception e){
                assertNotNull(e)
                assertTrue(e.message, e.message.contains("Option 'test1' value: [blah] does not allow multiple values"))
            }
            try{
                //should fail with list input
                testService.validateInputOptionValues(se2, ['option.test1': ['abc']])
                fail("Should have thrown exception")
            }catch (Exception e){
                assertNotNull(e)
                assertTrue(e.message, e.message.contains("Option 'test1' value: [abc] does not allow multiple values"))
            }
            try{
                //should fail with list input
                testService.validateInputOptionValues(se2, ['option.test1': ['abc','a']])
                fail("Should have thrown exception")
            }catch (Exception e){
                assertNotNull(e)
                assertTrue(e.message, e.message.contains("does not allow multiple values"))
            }
        }
        t:{
            //test multi-valued and list input
            assertTrue testService.validateInputOptionValues(se,[:])
            ScheduledExecution se2 = new ScheduledExecution()
            final Option option = new Option(name: 'test1', required:false, enforced: true, multivalued: true, delimiter: ' ')
            option.addToValues('a')
            option.addToValues('b')
            option.addToValues('abc')
            se2.addToOptions(option)
            assertNotNull(se2.options)
            assertEquals(1,se2.options.size())
            assertTrue testService.validateInputOptionValues(se2, ['option.test1': 'abc'])
            assertTrue testService.validateInputOptionValues(se2, ['option.test1': ['abc']])
            try{
                //should fail with invalid value input
                testService.validateInputOptionValues(se2, ['option.test1': ['blah']])
                fail("Should have thrown exception")
            }catch (Exception e){
                assertNotNull(e)
                assertTrue(e.message, e.message.contains("were not all in the allowed values"))
            }
            try{
                //should fail with invalid value input
                testService.validateInputOptionValues(se2, ['option.test1': ['abc','blah']])
                fail("Should have thrown exception")
            }catch (Exception e){
                assertNotNull(e)
                assertTrue(e.message, e.message.contains("were not all in the allowed values"))
            }
        }
        t: {
            //test multi-valued list with regex validation
            assertTrue testService.validateInputOptionValues(se, [:])
            ScheduledExecution se2 = new ScheduledExecution()
            final Option option = new Option(name: 'test1', required: false, enforced: false, multivalued: true, delimiter: ' ',regex:'^[abc]+$')
            se2.addToOptions(option)
            assertNotNull(se2.options)
            assertEquals(1, se2.options.size())
            assertTrue testService.validateInputOptionValues(se2, ['option.test1': 'abc'])
            assertTrue testService.validateInputOptionValues(se2, ['option.test1': ['abc']])
            try {
                //should fail with invalid regex value
                testService.validateInputOptionValues(se2, ['option.test1': 'zabc'])
                fail("Should have thrown exception")
            } catch (Exception e) {
                assertNotNull(e)
                assertTrue(e.message,e.message.contains("did not all match regular expression"))
            }
            try {
                //should fail with invalid regex value
                testService.validateInputOptionValues(se2, ['option.test1': ['blah']])
                fail("Should have thrown exception")
            } catch (Exception e) {
                assertNotNull(e)
                assertTrue(e.message,e.message.contains("did not all match regular expression"))
            }
            try {
                //should fail with invalid regex value
                testService.validateInputOptionValues(se2, ['option.test1': ['abc', 'blah']])
                fail("Should have thrown exception")
            } catch (Exception e) {
                assertNotNull(e)
                assertTrue(e.message,e.message.contains("did not all match regular expression"))
            }
        }
    }

    void testParseJobOptsFromString() {
        mockDomain(ScheduledExecution)
        mockDomain(Option)
        ScheduledExecution se = new ScheduledExecution()
        def testService = new ExecutionService()
        def frameworkService = new FrameworkService()
        testService.frameworkService = frameworkService

        t: {
            //test regex and optional value
            assertTrue testService.validateInputOptionValues(se, [:])
            ScheduledExecution se2 = new ScheduledExecution()
            se2.addToOptions(new Option(name: 'test1', enforced: false, multivalued: true, delimiter: ","))
            final opt2 = new Option(name: 'test2', enforced: true, multivalued: true, delimiter: ' ')
            opt2.addToValues('a')
            opt2.addToValues('b')
            opt2.addToValues('abc')
            se2.addToOptions(opt2)
            assertNotNull(se2.options)
            assertEquals(2, se2.options.size())

            final map = testService.parseJobOptsFromString(se2, "-test1 blah")
            assertNotNull map
            assertNotNull map['test1']
            assertTrue map['test1'] instanceof Collection
            assertEquals 1, map.size()
            assertEquals "wrong value: ${map['test1']}",1, map['test1'].size()
            assertEquals ("Wrong value: ${map.get('test1')}",["blah"], map.get('test1'))

            final map2 = testService.parseJobOptsFromString(se2, "-test1 blah,zah")
            assertNotNull map2
            assertNotNull map2['test1']
            assertTrue map2['test1'] instanceof Collection
            assertEquals 1, map2.size()
            assertEquals 2, map2['test1'].size()
            assertEquals (['blah','zah'], map2.get('test1'))

            final map3 = testService.parseJobOptsFromString(se2, "-test2 'blah zah nah'")
            assertNotNull map3
            assertNotNull map3['test2']
            assertTrue map3['test2'] instanceof Collection
            assertEquals 1, map3.size()
            assertEquals 3, map3['test2'].size()
            assertEquals (['blah','zah','nah'], map3.get('test2'))
        }
    }

    void testGenerateJobArgline() {
        mockDomain(ScheduledExecution)
        mockDomain(Option)
        ScheduledExecution se = new ScheduledExecution()
        def testService = new ExecutionService()
        def frameworkService = new FrameworkService()
        testService.frameworkService = frameworkService

        t: {
            //test regex and optional value
            ScheduledExecution se2 = new ScheduledExecution()
            se2.addToOptions(new Option(name: 'test1', enforced: false, multivalued: true,delimiter: "+"))
            se2.addToOptions(new Option(name: 'test2', enforced: false, multivalued: true))
            se2.addToOptions(new Option(name: 'test3', enforced: false, multivalued: false))
            assertNotNull(se2.options)
            assertEquals(3, se2.options.size())

            assertEquals "-test3 'some value'", ExecutionService.generateJobArgline(se2, ['test3': 'some value'])
            assertEquals "-test2 'some value'", ExecutionService.generateJobArgline(se2, ['test2': 'some value'])
            assertEquals "-test1 'some value'", ExecutionService.generateJobArgline(se2, ['test1': 'some value'])
            //multivalue
            assertEquals "-test1 'some value+another value'", ExecutionService.generateJobArgline(se2, ['test1': ['some value','another value']])
            assertEquals "-test2 'some value,another value'", ExecutionService.generateJobArgline(se2, ['test2': ['some value','another value']])
            assertEquals "-test3 'some value,another value'", ExecutionService.generateJobArgline(se2, ['test3': ['some value','another value']])
        }
    }

    /**
     * Test createContext method
     */
    void testCreateContext(){
        mockDomain(Execution)
        mockDomain(User)
        def testService = new ExecutionService()
        def frameworkService = new FrameworkService()
        testService.frameworkService = frameworkService
        //create mock user
        User u1 = new User(login: 'testuser')
        u1.save()

        t:{//basic test

            Execution se = new Execution(argString:"-test args",user:"testuser",project:"testproj", loglevel:'WARN',doNodedispatch: false)
            def val=testService.createContext(se,null,null,null,null,null)
            assertNotNull(val)
            assertNotNull(val.nodeSet)
            assertEquals("testproj",val.frameworkProject)
            assertEquals("testuser",val.user)
            assertEquals(['-test','args'] as String[],val.args)
            assertEquals(1,val.loglevel)
            assertNull(val.framework)
            assertNull(val.executionListener)
        }
        t:{//test DNE user

            Execution se = new Execution(argString:"-test args",user:"DNEuser",project:"testproj", loglevel:'WARN',doNodedispatch: false)
            try {
                def val=testService.createContext(se,null,null,null,null,null)
                fail("Should not succeed")
            } catch (Exception e) {
                assertEquals("User DNEuser is not authorized to run this Job.",e.message)
            }

        }
        t: {//check datacontext

            Execution se = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false)
            def val = testService.createContext(se, null, null, null, null, null)
            assertNotNull(val)
            assertNotNull(val.dataContext)
            assertNotNull(val.dataContext.job)
            assertEquals(0,val.dataContext.job.size())
            assertNotNull(val.dataContext.option)
            assertEquals([test:"args"],val.dataContext.option)
        }
        t: {//check datacontext, inputargs instead of argString

            Execution se = new Execution(user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false)
            def val = testService.createContext(se, null, null, null, null, ['-test','args','-test2','monkey args'] as String[])
            assertNotNull(val)
            assertNotNull(val.dataContext)
            assertNotNull(val.dataContext.job)
            assertEquals(0,val.dataContext.job.size())
            assertNotNull(val.dataContext.option)
            assertEquals([test:"args",test2:'monkey args'],val.dataContext.option)
        }
        t: {//check datacontext, include job data

            Execution se = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false)
            def val = testService.createContext(se, null, null, [id:"3",name:"testjob"], null, null)
            assertNotNull(val)
            assertNotNull(val.dataContext)
            assertNotNull(val.dataContext.job)
            assertEquals([id: "3", name: "testjob"],val.dataContext.job)
            assertNotNull(val.dataContext.option)
            assertEquals([test:"args"],val.dataContext.option)
        }
        t: {//check nodeset, empty

            Execution se = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: false)
            def val = testService.createContext(se, null, null, [id:"3",name:"testjob"], null, null)
            assertNotNull(val)
            assertNotNull(val.nodeSet)
            assertNull(val.nodeSet.include)
            assertNull(val.nodeSet.exclude)
        }
        t: {//check nodeset, filtered from execution obj. include name

            Execution se = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: true, nodeIncludeName: "testnode")
            def val = testService.createContext(se, null, null, [id: "3", name: "testjob"], null, null)
            assertNotNull(val)
            assertNotNull(val.nodeSet)
            assertNotNull(val.nodeSet.exclude)
            assertNotNull(val.nodeSet.include)
            assertNull(val.nodeSet.exclude.name)
            assertEquals("testnode", val.nodeSet.include.name)
        }
        t: {//check nodeset, filtered from execution obj. exclude name

            Execution se = new Execution(argString: "-test args", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: true, nodeExcludeName: "testnode")
            def val = testService.createContext(se, null, null, [id: "3", name: "testjob"], null, null)
            assertNotNull(val)
            assertNotNull(val.nodeSet)
            assertNotNull(val.nodeSet.exclude)
            assertNotNull(val.nodeSet.include)
            assertEquals("testnode", val.nodeSet.exclude.name)
            assertNull(val.nodeSet.include.name)
        }
    }

    /**
     * Test use of ${option.x} and ${job.y} parameter expansion in node filter tag and name filters.
     */
    void testCreateContextParameterizedFilters() {
        mockDomain(Execution)
        mockDomain(User)
        def testService = new ExecutionService()
        def frameworkService = new FrameworkService()
        testService.frameworkService = frameworkService
        //create mock user
        User u1 = new User(login: 'testuser')
        u1.save()

        t: {//basic test

            Execution se = new Execution(argString: "-test args -test3 something", user: "testuser", project: "testproj", loglevel: 'WARN', doNodedispatch: true,nodeIncludeName: "basic")
            def val = testService.createContext(se, null, null, [id:'3',name:'blah',group:'something/else',username:'bill',project:'testproj'], null, null)
            assertNotNull(val)
            assertNotNull(val.nodeSet)
            assertNotNull(val.nodeSet.exclude)
            assertNotNull(val.nodeSet.include)
            assertNull(val.nodeSet.exclude.tags)
            assertNull(val.nodeSet.exclude.name)
            assertNull(val.nodeSet.include.tags)
            assertEquals("basic", val.nodeSet.include.name)
        }
        t: {//variable expansion in include name

            Execution se = new Execution(argString: "-test args -test3 something", user: "testuser", project: "testproj", loglevel: 'WARN',
                doNodedispatch: true,
                nodeInclude: "a,\${option.test} \${option.test3}",
                nodeIncludeName: "b,\${option.test} \${option.test3}",
                nodeIncludeTags: "c,\${option.test} \${option.test3}",
                nodeIncludeOsArch: "d,\${option.test} \${option.test3}",
                nodeIncludeOsFamily: "e,\${option.test} \${option.test3}",
                nodeIncludeOsName: "f,\${option.test} \${option.test3}",
                nodeIncludeOsVersion: "g,\${option.test} \${option.test3}",
                nodeExclude: "h,\${job.id} \${job.name} \${job.group} \${job.username} \${job.project}",
                nodeExcludeName: "i,\${job.id} \${job.name} \${job.group} \${job.username} \${job.project}",
                nodeExcludeTags: "j,\${job.id} \${job.name} \${job.group} \${job.username} \${job.project}",
                nodeExcludeOsArch: "k,\${job.id} \${job.name} \${job.group} \${job.username} \${job.project}",
                nodeExcludeOsFamily: "l,\${job.id} \${job.name} \${job.group} \${job.username} \${job.project}",
                nodeExcludeOsName: "m,\${job.id} \${job.name} \${job.group} \${job.username} \${job.project}",
                nodeExcludeOsVersion: "n,\${job.id} \${job.name} \${job.group} \${job.username} \${job.project}",
            )
            def val = testService.createContext(se, null, null, [id:'3',name:'blah',group:'something/else',username:'bill',project:'testproj'], null, null)
            assertNotNull(val)
            assertNotNull(val.nodeSet)
            assertNotNull(val.nodeSet.exclude)
            assertNotNull(val.nodeSet.include)
            assertEquals("a,args something", val.nodeSet.include.hostname)
            assertEquals("b,args something", val.nodeSet.include.name)
            assertEquals("c,args something", val.nodeSet.include.tags)
            assertEquals("d,args something", val.nodeSet.include.osarch)
            assertEquals("e,args something", val.nodeSet.include.osfamily)
            assertEquals("f,args something", val.nodeSet.include.osname)
            assertEquals("g,args something", val.nodeSet.include.osversion)
            assertEquals("h,3 blah something/else bill testproj", val.nodeSet.exclude.hostname)
            assertEquals("i,3 blah something/else bill testproj", val.nodeSet.exclude.name)
            assertEquals("j,3 blah something/else bill testproj", val.nodeSet.exclude.tags)
            assertEquals("k,3 blah something/else bill testproj", val.nodeSet.exclude.osarch)
            assertEquals("l,3 blah something/else bill testproj", val.nodeSet.exclude.osfamily)
            assertEquals("m,3 blah something/else bill testproj", val.nodeSet.exclude.osname)
            assertEquals("n,3 blah something/else bill testproj", val.nodeSet.exclude.osversion)
        }
    }
}
