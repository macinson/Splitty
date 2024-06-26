# Agenda week 8

---

~ 50 min

---
| Key | Value                |
| --- |----------------------|
| Date: | 02/04/2024           |
| Time: | 15:45                |
| Location: | Drebbelweg PC-hall 1 |
| Chair | Maciej Kozik         |
| Minute Taker | Julian Overmars                 |
| Attendees: | Everyone             |

## Opening by chair (1 min)
- Check if everyone's present
- Short introduction to the meeting
## Check-in (3 min)
- How's everybody doing?
- Everybody summarizes their work from the previous week
- Did anybody encounter any problems?
- Assess the execution of previous week's plan
## Announcements by the TA (2 min)
## Presentation of the current state of the app to the TA (5 min)
Show the following:
- joining/creating/editing an event
- managing expenses
- the refactored invite code
- progress on the HCI requirements
- file manager
## Discuss current issues/bugs (10 min)
- Not all scenes are being refreshed
- Using Config class vs using SettingsCtrl
- Some scenes changed, hence HCI requirements are not fully done for them
- For the aforementioned scenes, the language switch needs to be done as well
- If the user wants to join an event, and they have not specified their name, the Exception is thrown.
The message on the client is "event code not found".
- If anybody encountered anything else, that's the time to mention it
## Plan for this week (20 min)

#### Completing the backlog (10 min):

- Long-polling
- Email notifications
- Display invite code
- Edit/delete participants
- Edit event should not require to input event id
- Display share of expenses per participant
- Finish admin
- Finish language
- Display correct currency in expenses
- Add entrance fees tag, add custom tags, color coding for tags etc.

#### Other assignments (10 min)

- Technology: do we want to use more of websockets and long-polling (among others), service classes, dependency injection everywhere
- Testing: bump up line coverage
- HCI: finish it for all scenes (including error handling)

## Summary of meeting (4 min)

## Feedback for the meeting (2 min)
## Any questions? + Closure (3 min)

## this weeks goals:
 - finish everything, cleanup application is next week

## what couldn't be done last week:
 - undo operations and Service classes.

## Bug fixes and implementations to do the following weeks:

 - clear text fields when changing windows -> joris
 - HCI requirements for new scenes -> everyone
 - language switch finalize -> joris
 - when joining event make a new participant -> yavor and julian
 - a host can add ghost participants -> yavor and julian
 - only host can change ghost participants and no one else and the rest only can change themselves. -> yavor and julian all the participant
 - a host can delete the event -> yavor and julian
 - testing when you have free time -> everyone
 - email notifications -> Stijn
 - long polling -> Maciej
 - finishing expenses -> Maciej
 - currency display -> Maciej
 - json dumps -> jesse
 - creating events -> joris
 - HCI requirements -> julian

## important decision 
 - Names are nullable and the participant cannot change their name per event. If the name is null just put unknown. Make sure its updated
    when the settings are updated. 

## polishing week 9:
 - statistics percentages
 - testing: 80%
 - css