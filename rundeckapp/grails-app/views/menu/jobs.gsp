<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="jobs"/>
    <title><g:message code="main.app.name"/></title>
    <script type="text/javascript">

        function execSubmit(elem){
            var params=Form.serialize(elem);
            new Ajax.Request(
                '${createLink(controller:"scheduledExecution",action:"runJobInline")}', {
                parameters: params,
                evalScripts:true,
                onComplete: function(trans) {
                    var result={};
                    if(trans.responseJSON){
                        result=trans.responseJSON;
                    }else if(trans.responseText){
                        result=eval(trans.responseText);
                    }
                    if(result.id){
                        unloadExec();
                    }else if(result.error=='invalid'){
                        //reload form for validation
                        loadExec(null,params+"&dovalidate=true");
                    }else{
                        unloadExec();
                        showError(result.message?result.message:result.error?result.error:"Failed request");
                    }
                },
                onFailure: requestError.curry("runJobInline")
            });
        }
        function loadedFormSuccess(){
            if ($('execFormCancelButton')) {
                $('execFormCancelButton').onclick = function() {
                    unloadExec();
                    return false;
                };
                $('execFormCancelButton').name = "_x";
            }else{
                console.log("no");
            }
            if ($('execFormRunButton')) {
                $('execFormRunButton').onclick = function(evt) {
                    Event.stop(evt);
                    execSubmit('execDivContent');
                    return false;
                };
            }
            $('indexMain').hide();
            $('execDiv').show();
            $('busy').hide();
        }
        function loadExec(id,eparams) {
            $('busy').innerHTML = '<img src="' + appLinks.iconSpinner + '" alt=""/> Loading...';
            $('busy').show();
            $("error").hide();
            var params=eparams;
            if(!params){
                params={id:id};
            }
            new Ajax.Updater(
                'execDivContent',
                '${createLink(controller:"scheduledExecution",action:"executeFragment")}', {
                parameters: params,
                evalScripts:true,
                onComplete: function(transport) {
                    if (transport.request.success()) {
                        loadedFormSuccess();
                    }
                },
                onFailure: requestError.curry("executeFragment for [" + id + "]")
            });

        }
        function unloadExec(){
            $('execDiv').hide();
            $('indexMain').show();
            $('execDivContent').innerHTML='';
            $('busy').hide();
        }

        function requestError(item,trans){
            unloadExec();
            showError("Failed request: "+item+" . Result: "+trans.getStatusText());
        }
        function showError(message){
             $('error').innerHTML+=message;
             $("error").show();
        }
       


        //set box filterselections
        function setFilter(name,value){
            if(!value){
                value="!";
            }
            var str=name+"="+value;
            new Ajax.Request("${createLink(controller:'user',action:'addFilterPref')}",{parameters:{filterpref:str}, evalJSON:true,onSuccess:function(response){
                _setFilterSuccess(response,name);
            }});
        }
        function _setFilterSuccess(response,name){
            var data=eval("("+response.responseText+")"); // evaluate the JSON;
            if(data){
                var bfilters=data['filterpref'];
                //reload page
                document.location="${createLink(controller:'menu',action:'workflows')}"+(bfilters[name]?"?filterName="+encodeURIComponent(bfilters[name]):'');
            }
        }
        var savedcount=0;
        function _pageUpdateNowRunning(count){
            if(count!=savedcount){
                savedcount=count;
                loadHistory();
            }
        }

        /** START history
         *
         */
        function loadHistory(){
            new Ajax.Updater('histcontent',"${createLink(controller:'reports',action:'eventsFragment')}",{
                parameters:{compact:true,nofilters:true,jobIdFilter:'!null',recentFilter:'1d',userFilter:'${session.user}',projFilter:'${session.project}'},
                evalScripts:true,
                onComplete: function(transport) {
                    if (transport.request.success()) {
                        Element.show('histcontent');
                    }
                },
            });
        }

        //now running
        var runupdate;
        function loadNowRunning(){
            runupdate=new Ajax.PeriodicalUpdater('nowrunning','${createLink(controller:"menu",action:"nowrunningFragment")}',{
                evalScripts:true,
                parameters:{},
            });
        }


        /////////////
        // Job context detail popup code
        /////////////

        var doshow=false;
        var popvis=false;
        var lastHref;
        var targetLink;
        function popJobDetails(elem){
            if(doshow && $('jobIdDetailHolder')){
                new MenuController().showRelativeTo(elem,$('jobIdDetailHolder'),-20,16);
                popvis=true;
                if(targetLink){
                    $(targetLink).removeClassName('glow');
                    targetLink=null;
                }
                $(elem).addClassName('glow');
                targetLink=elem;
            }
        }
        function showJobDetails(elem){
            //get url
            var href=elem.href;
            var match=href.match(/\/job\/show\/(.+)$/);
            if(!match){
                return;
            }
            lastHref=href;
            doshow=true;
            //match is id
            var matchId=match[1];
            var viewdom=$('jobIdDetailHolder');
            var bcontent=$('jobIdDetailContent');
            if(!viewdom){
                viewdom = $(document.createElement('div'));
                viewdom.addClassName('bubblewrap');
                viewdom.setAttribute('id','jobIdDetailHolder');
                viewdom.setAttribute('style','display:none;');

                var btop = document.createElement('div');
                btop.addClassName('bubbletop');
                viewdom.appendChild(btop);
                bcontent = $(document.createElement('div'));
                bcontent.addClassName('bubblecontent');
                bcontent.setAttribute('id','jobIdDetailContent');
                viewdom.appendChild(bcontent);
                document.body.appendChild(viewdom);
                Event.observe(viewdom,'mouseover',bubbleMouseover);
                Event.observe(viewdom,'mouseout',jobLinkMouseout.curry(viewdom));
            }
            bcontent.loading();


            new Ajax.Updater('jobIdDetailContent','${createLink(controller:'scheduledExecution',action:'detailFragment')}',{
                parameters:{id:matchId},
                evalScripts:true,
                onComplete: function(trans){
                    if(trans.request.success()){
                        popJobDetails(elem);
                    }
                },
                onFailure: function(trans){
                    bcontent.innerHTML='';
                    viewdom.hide();
                }
            });
        }
        var motimer;
        function bubbleMouseover(evt){
            if(mltimer){
                clearTimeout(mltimer);
                mltimer=null;
            }
        }
        function jobLinkMouseover(elem,evt){
            if(mltimer){
                clearTimeout(mltimer);
                mltimer=null;
            }
            if(motimer){
                clearTimeout(motimer);
                motimer=null;
            }
            if(popvis && lastHref==elem.href){
                return;
            }
            var delay=500;
            if(popvis){
                delay=50;
            }
            motimer=setTimeout(showJobDetails.curry(elem),delay);
        }
        var mltimer;
        function jobLinkMouseout(elem,evt){
            //hide job details
            if(motimer){
                clearTimeout(motimer);
                motimer=null;
            }
            doshow=false;
            mltimer=setTimeout(doMouseout,3000);
        }
        function doMouseout(){
            if(popvis && $('jobIdDetailHolder')){
                popvis=false;
                $('jobIdDetailHolder').hide();
            }
            if(targetLink){
                $(targetLink).removeClassName('glow');
                targetLink=null;
            }
        }
        function initJobIdLinks(){
            $$('a.jobIdLink').each(function(e){
                Event.observe(e,'mouseover',jobLinkMouseover.curry(e));
                Event.observe(e,'mouseout',jobLinkMouseout.curry(e));
            });
        }
        function init(){
            loadNowRunning();
            initJobIdLinks();
            Event.observe(document.body,'click',function(evt){
                //click outside of popup bubble hides it
                var t = $(evt.element()).up('#jobIdDetailHolder');
                if(!t && !evt.element().onclick && !evt.element().onmousedown){
                    doMouseout();
                }
                return true;
            },false);
            Event.observe(document.body,'keydown',function(evt){
                //escape key hides popup bubble
                if(evt.keyCode==27 ){
                    doMouseout();
                }
                return true;
            },false);
        }
        Event.observe(window,'load',init);
    </script>
    <g:javascript library="yellowfade"/>
    <g:render template="/framework/remoteOptionValuesJS"/>
    <style type="text/css">
    #nowrunning{
        max-height: 150px;
        overflow-y: auto;
        margin: 0 0 10px 0;
    }
    .error{
        color:red;
    }
    a.glow{
        background: #cfc;
        border-radius:3px;
        -moz-border-radius:3px;
        -webkit-border-radius:3px;
    }
    .bubblewrap{
        position:absolute;
        width: 500px;
        height: 250px;
    }
    .bubbletop{
        height:16px;
        background: transparent url(${resource(dir:'images',file:'bubble-bg.png')}) top 10px no-repeat;
        z-index: 1;
    }
    .bubblecontent{
        position:relative;
        border:1px solid #aaa;
        background:white;
        padding: 10px;
        margin-top:-1px;
        z-index: -1;
        border-radius:10px;
        -moz-border-radius:10px;
        -webkit-border-radius:10px;
        -moz-box-shadow:#aaa 2px 2px 7px;
        -webkit-box-shadow:#aaa 2px 2px 7px;
    }
    </style>
</head>
<body>


<div class="pageBody solo" id="indexMain">
    <g:if test="${flash.savedJob}">
        <div style="margin-bottom:10px;">
        <span class="popout message note" style="background:white">
            ${flash.savedJobMessage?flash.savedJobMessage:'Saved changes to Job'}:
            <g:link controller="scheduledExecution" action="show" id="${flash.savedJob.id}">${flash.savedJob.jobName}</g:link>
        </span>
        </div>
        <g:javascript>
            fireWhenReady('jobrow_${flash.savedJob.id}',doyft.curry('jobrow_${flash.savedJob.id}'));

        </g:javascript>
    </g:if>
    <span class="prompt">Now running <span class="nowrunningcount">(0)</span></span>
    <div id="nowrunning"></div>
    <div id="error" class="error" style="display:none;"></div>
    <g:render template="workflowsFull" model="${[jobgroups:jobgroups,wasfiltered:wasfiltered?true:false,nowrunning:nowrunning,nextExecutions:nextExecutions,jobauthorizations:jobauthorizations,authMap:authMap,nowrunningtotal:nowrunningtotal,max:max,offset:offset,paginateParams:paginateParams,sortEnabled:true]}"/>
</div>
<div id="execDiv" style="display:none">

    <div id="execDivContent" >

    </div>
</div>
<div class="runbox">History</div>
    <div class="pageBody">
        <div id="histcontent"></div>
        <g:javascript>
            fireWhenReady('histcontent',loadHistory);
        </g:javascript>
    </div>
</body>
</html>
