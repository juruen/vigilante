Vigilante API
=============

Data modelling
--------------

The reference implementation of Vigilante API uses Googles Datastore as a
backend.

As mentioned earlier, the requirements of the project are to scale up to
thousands of companies with thousands of users in each company.

#### First iteration

In this first iteration our design may resemble a relation DB design.

##### Basic objects

###### User

| Field         | Type         |  Reference |
|-------------- |--------------|------------|
| name          | string       |            |
| email         | string       |            |
| timeZone      | string       |            |
| team          | [long]       |  team      |
| schedule      | [long]       |  schedule  |

###### Team

| Field         | Type         |  Reference       |
|-------------- |--------------|------------------|
| name          | string       |                  |
| users         | [long]       |   user           |
| escalations   | [long]       |   escalation     |
| services      | [long]       |   service        |


###### Schedule

| Field         | Type         |  Reference   |
|-------------- |--------------|--------------|
| name          | string       |              |
| start         | long         |              |
| length        | long         |              |
| timeRanges    | timeRanges   |              |
| users         | [long]       |  user        |
| escalation    | [long]       |  escalation  |

###### Escalation

| Field         | Type         |  Reference |
|-------------- |--------------|------------|
| name          | string       |            |
| schedules     | [long]       |  schedule  |
| services      | [long]       |  service   |
| teams         | [long]       |  team      |

##### Service

| Field         | Type         |  Reference   |
|-------------- |--------------|--------------|
| name          | string       |              |
| serviceKey    | string       |              |
| team          | long         |  user        |
| escalation    | long         |  escalation  |

##### Main use cases

When designing an API, especially when achieving high performance is a
requirement, it is important to not design the API in isolation of the use
cases, and instead to have them into consideration since the very beginning
to make sure that performance is guaranteed for the main use of the API.

What follows is a description of the main use cases of the API through
the interaction of a user.

We'll use these cases to make sure that performance is within acceptable
parameters.

###### Show users in my team

Display a list of users in a my teams containing:

- Team name

###### Read operations

| id      | #requests  | Type       | description              |
|---------|------------|------------|--------------------------|
|  1      | 1          | filter     | teams for user           |
|  2      | 1          | batch key  | temas in user            |
| total   | 2          |            |                          |

Dependencies *1->2*

###### Show my schedules

Display a list of my schedules containing:

- Teams that are using each listed schedule
- Escalations that are using each listed schedule


###### Read operations

| id      | #requests  | Type       | description              |
|---------|------------|------------|--------------------------|
|  1      | 1          | filter     | schedules for a user     |
|  2      | 1          | batch key  | escalations in schedules |
|  3      | 1          | batch key  | teams in escalations     |
| total   | 3          |            |                          |

Dependencies *1->2->3*.

###### Show my escalations

Display a list of my escalations containing:

- Schedule names used in each escalation
- Service names used in each escalation


###### Read operations

| id      | #requests  | Type       | description              |
|---------|------------|------------|--------------------------|
|  1      | 1          | filter     | teams for user           |
|  2      | 1          | batch key  | escalations in teams     |
|  3      | 1          | batch key  | schedules in escalations |
|  4      | 1          | batch key  | services in escalations  |
| total   | 4          |            |                          |

Dependencies *1->2->[3, 4]*.


###### Show my services

Display a list of my services containing:

- Escalation name used in each service
- Team name used in service


####### Read operations

| id      | #requests  | Type       | description              |
|---------|------------|------------|--------------------------|
|  1      | 1          | filter     | teams for user           |
|  2      | 1          | batch key  | services in teams        |
|  3      | 1          | batch key  | escalation in teams      |
| total   | 3          |            |                          |

Dependencies *1->[2, 3]*

#### Second iteration

We can do better in terms of read requests. The trick is to realize that
most of the time we requiere extra requests to feth the name of
the objects that are being referenced in the relationships.

How about we denormalize our design, and we add these names directly with
the id of the object reference. This way, we won't need to issue request to
fetch them.

The drawback of this approach is that we need to do more work to keep
this denormalized data when modifing the original object. We will address
the shortcomings of this approach later on when we talk about keep referntial
integrity.

We are going to be a bit conservative, and we are going to start by
denormalizing only those objects that are used in the use cases where we
end up with more than 2 requests.

##### Basic objects

###### User

| Field         | Type            |  Reference |
|-------------- |-----------------|------------|
| name          | string          |            |
| email         | string          |            |
| timeZone      | string          |            |
| team          | [string]        |  team      |
| schedule      | [string]        |  schedule  |

###### Team

| Field         | Type         |  Reference       |
|-------------- |--------------|------------------|
| name          | string       |                  |
| users         | [long]       |   user           |
| escalations   | [long]       |   escalation     |
| services      | [long]       |   service        |


###### Schedule

| Field         | Type            |  Reference   |
|-------------- |-----------------|--------------|
| name          | string          |              |
| start         | long            |              |
| length        | long            |              |
| timeRanges    | timeRanges      |              |
| users         | [{long:string}] |  user        |
| escalation    | [{long:string}] |  escalation  |

###### Escalation

| Field         | Type            |  Reference |
|-------------- |-----------------|------------|
| name          | string          |            |
| schedules     | [{long:string}] |  schedule  |
| services      | [{long:string}] |  service   |
| teams         | [string]        |  team      |

##### Service

| Field         | Type         |  Reference   |
|-------------- |--------------|--------------|
| name          | string       |              |
| serviceKey    | string       |              |
| team          | string,long  |  team        |
| escalation    | string,long  |  escalation  |

##### Main use cases

When designing an API, especially when achieving high performance is a
requirement, it is important to not design the API in isolation of the use
cases, and instead to have them into consideration since the very beginning
to make sure that performance is guaranteed for the main use of the API.

What follows is a description of the main use cases of the API through
the interaction of a user.

We'll use these cases to make sure that performance is within acceptable
parameters.

###### Show users in my team

Display a list of users in a my teams containing:

- Team name

####### Read operations

| id      | #requests  | Type       | description              |
|---------|------------|------------|--------------------------|
|  1      | 1          | filter     | teams for user           |
|  2      | 1          | batch key  | temas in user            |
| total   | 2          |            |                          |

Dependencies *1->2*

###### Show my schedules

Display a list of my schedules containing:

- Teams that are using each listed schedule
- Escalations that are using each listed schedule


####### Read operations

| id      | #requests  | Type       | description              |
|---------|------------|------------|--------------------------|
|  1      | 1          | filter     | schedules for a user     |
| total   | 1          |            |                          |


###### Show my escalations

Display a list of my escalations containing:

- Schedule names used in each escalation
- Service names used in each escalation


###### Read operations

| id      | #requests  | Type       | description              |
|---------|------------|------------|--------------------------|
|  1      | 1          | filter     | teams for user           |
|  2      | 1          | batch key  | escalations in teams     |
| total   | 2          |            |                          |

Dependencies *1->2*


###### Show my services

Display a list of my services containing:

- Escalation name used in each service
- Team name used in service

###### Read operations

| id      | #requests  | Type       | description              |
|---------|------------|------------|--------------------------|
|  1      | 1          | filter     | teams for user           |
|  2      | 1          | batch key  | services in teams        |
| total   | 2          |            |                          |

Dependencies *1->2*
