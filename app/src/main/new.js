rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Forms: Only the owner can see/edit their templates
    match /forms/{formId} {
      allow read, update, delete: if request.auth != null && request.auth.uid == resource.data.userId;
      allow create: if request.auth != null;
    }
    
    // Submissions: 
    // 1. The person who filled it out (userId) can see/edit it
    // 2. The person who created the form (creatorId) can READ it
    match /submissions/{submissionId} {
      allow read: if request.auth != null && (request.auth.uid == resource.data.userId || request.auth.uid == resource.data.creatorId);
      allow update, delete: if request.auth != null && request.auth.uid == resource.data.userId;
      allow create: if request.auth != null;
    }

    match /imports/{importId} {
      allow read, update, delete: if request.auth != null && request.auth.uid == resource.data.userId;
      allow create: if request.auth != null;
    }
  }
}