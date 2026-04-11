# FormFlow

#### FormFlow is a powerful library for building conversational bots using the Microsoft Bot Framework. It allows developers to create complex dialog flows with ease, enabling users to interact with bots in a natural and intuitive way.

#### contextpath: /formflow port_no:8082 (eg: http://localhost:8082/formflow/)

### Swagger UI: http://localhost:8082/formflow/swagger-ui/index.html

## API endpoints:

### Authentication APIs:

- existing username check -- http://localhost:8082/formflow/auth/usernameCheck  (post)
- existing email check -- http://localhost:8082/formflow/auth/emailCheck  (post)

- signup -- http://localhost:8082/formflow/auth/signup  (post)
- verify Account -- http://localhost:8082/formflow/auth/verifyAccount  (post)
- Resend OTP for account verification -- http://localhost:8082/formflow/auth/resendOtpVerifyaccount  (post)

- login -- http://localhost:8082/formflow/auth/login  (post)
- refresh Token -- http://localhost:8082/formflow/auth/refresh  (post)

- forget password -- http://localhost:8082/formflow/auth/forgotPassword (post)
- Resend OTP for forget password -- http://localhost:8082/formflow/auth/resendOtpResetPassword  (post)
- verify OTP for forget password -- http://localhost:8082/formflow/auth/verifyResetOtp  (post)
- reset password -- http://localhost:8082/formflow/auth/resetPassword  (post)

- logout -- http://localhost:8082/formflow/auth/logout  (post)

### Group APIs--
#### (get Access Token and Resfresh Token from login API and use it in header for below APIs)
- create group -- http://localhost:8082/formflow/group/createGroup  (post)
- get all group created by a user-- http://localhost:8082/formflow/group/myGroups  (get)
- get all Admin of a group -- http://localhost:8082/formflow/group/{groupId}/admins (get) id is group id
- get all Member of a group -- http://localhost:8082/formflow/group/{groupId}/members (get) id is group id
- add members to group -- http://localhost:8082/formflow/group/{groupId}/addMembers  (post) id is group id
- remove members from group -- http://localhost:8082/formflow/group/{groupId}/removeUsers  (post) id is group id and userId is user id
- add admins to group -- http://localhost:8082/formflow/group/{groupId}/addAdmins  (post) id is group id
- Demote Admin to Member -- http://localhost:8082/formflow/group/{groupId}/removeAdmins  (post) id is group id and userId is user id
- Invite members to group -- http://localhost:8082/formflow/group/{groupId}/invite  (post) id is group id
- Join group by invite code -- http://localhost:8082/formflow/group/joinByInviteCode  (post)


### User APIs--  
#### (get Access Token and Resfresh Token from login API and use it in header for below APIs)
- create form -- http://localhost:8082/formflow/user/createForm  (post)
- get all form -- http://localhost:8082/formflow/user/allForm  (get)
- get form by id -- http://localhost:8082/formflow/forms/{id}  get() id is form-id
- get form by status -- http://localhost:8082/formflow/user/status/{status}  status is form status (eg: published, draft)
- update form -- http://localhost:8082/formflow/user/updateForm/{id}  (put) id is form id
- moveToTrash form -- http://localhost:8082/formflow/user/form/moveToTrash/{id}  (patch) id is form-id
- get all trash form -- http://localhost:8082/formflow/user/form/trash  (get)
- restore form from trash -- http://localhost:8082/formflow/user/form/restoreFromTrash/{id}  (patch) id is form-id
- update form Details by id -- http://localhost:8082/formflow/user/updateForm/{id}  (put) id is form-id

### public APIs--for form Data
- get form by id -- http://localhost:8082/formflow/public/form/{id}  (get) id is form-id

### Admin APIs--
#### (get Access Token and Resfresh Token from login API and use it in header for below APIs)
- get all form -- http://localhost:8082/formflow/admin/getAllForms  (get)
- get all User -- http://localhost:8082/formflow/admin/getAllUsers  (get)

### Response APIs--
#### not authenticated -- anyone can access these APIs without authentication
- post response -- http://localhost:8082/formflow/api/responses  (post)
- get form response by formId -- http://localhost:8082/formflow/api/responsesget/{formId} (get)
