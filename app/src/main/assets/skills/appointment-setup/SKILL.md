---
name: appointment-setup
description: Set up an appointment end to end: find a slot, book it, put it in the calendar with a reminder and the address.
---

# Appointment setup

Use when the user wants an appointment booked or a visit planned: a doctor, a barber, a repair, a meeting.

1. Work out what is needed: who, what kind of appointment, where, which days work, and how long it takes.
2. Look at the calendar first: find the days that are actually free. Do not book over something already there.
3. Open the app that makes the appointment and find the real openings. Read them from the screen; do not guess a time.
4. Before booking, show the user the exact slot, the place and the address, and ask for the OK.
5. After the booking, add it to the calendar with the name, the time, the address and a reminder (15 minutes before, unless the
   user says otherwise). Say which calendar and which reminder you used.
6. If the booking needs a phone number, a code or a payment, hand that step to the user.

## Rules
- Never book, pay or confirm anything before the user approves that exact slot.
- Two appointments at once is a bug: check the calendar again before saving.
- If no slot fits, say so and offer the two closest ones instead of booking the first one that appears.
