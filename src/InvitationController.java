///**
// * @author Marcello De Bernardi
// *
// * An InvitationController object is used to handle sending invitations to the
// * cutest woman ever. It is a singleton class, as only one invitation controller
// * can be created (there is only one person in the world to invite).
// */
//public class InvitationController {
//    // constants
//    private static final int CUTENESS = Integer.MAX_VALUE;
//    private static final int LOVE = Integer.MAX_VALUE;
//    // singleton instance
//    private static InvitationController instance;
//
//
//    /**
//     * Zero-argument private constructor for singleton instance
//     */
//    private InvitationController() {}
//
//
//    /**
//     * Returns a reference to the instance
//     * @return instance
//     */
//    public static InvitationController getInstance() {
//        if (instance == null) instance = new InvitationController();
//        return instance;
//    }
//
//    /**
//     * Invites the target and returns result of invitation
//     * @param person lovely person to invite
//     * @return true if accepts invitation, false otherwise
//     */
//    public boolean invite(Catherine instance) {
//        // invites person with the given message, amount of cuteness, amount of love
//        return instance.invite(
//                "Dear " + instance.getName() + ",\n"
//                        + "I know today hasn't been the best of days. I've been by your side and watched "
//                        + "you confront adversity and stress with strength and determination.\n"
//                        + "I was going to write this by hand, but I realized I didn't have a pen. Also, you'd "
//                        + "have known immediately what I was up to, because you know me really, really well.\n"
//                        + "I was wondering if, at some point after bashing, on your way home, you wanted to take "
//                        + "a 10-minute stroll in Mile End Park. I know you're not too keen on that park, but "
//                        + "I'd like to revisit the place of our second kiss. Our first kiss in Whitechapel was "
//                        + "adorably awkward, but our second kiss in Mile End Park was passionate. I kissed you by "
//                        + "surprise, suddenly turning at you, holding you. I remember you had time to say "
//                        + "'Marcello ...' right before I kissed you. Maybe you were going to say no, or maybe I just "
//                        + "started you. But then we kissed, and you promptly fell apart.\n"
//                        + "I'd like to revisit that spot to trigger in you a happy memory, in the way that only "
//                        + "actually being in the spot can do. To make you smile and journey home with something "
//                        + "nice on your mind.\n",
//                CUTENESS,
//                LOVE
//        );
//
//    }
//}
