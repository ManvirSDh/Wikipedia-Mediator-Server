package cpen221.mp3.fsftbuffer;

    //Testing Task 1 using BufferableString
    public class BufferableString implements Bufferable
    {
        private final String s;

        //creating a new BufferableString
        public BufferableString(String str) {
            this.s = str;
        }

        @Override
        public String id() {
            return String.valueOf(this.s.hashCode());
        }



        @Override
        public boolean equals(Object that) {
            if (this == that) {
                return true;
            }
            if (!(that instanceof BufferableString)) {
                return false;
            }
            BufferableString b = (BufferableString) that;
            return this.id().equals(b.id());
        }
    }

