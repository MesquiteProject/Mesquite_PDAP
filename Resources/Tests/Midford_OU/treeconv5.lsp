(SETF *READTABLE-CASE* :PRESERVE)

;;;;;;;;;; VALUE FOR BLOCK STRUCTURE  TOLERANCE. YOU MAY NEED TO TWEAK THIS IF THE PROGRAM DOES NOT RUN CORRECTLY ;;;;;;;;;

(DEF *TOLERANCE* 0.00000000001)

;;;;;;;;;;;;; MACROS ;;;;;;;;;;;

(DEFMACRO FOR (VAR START STOP &BODY BODY)
	(LET ((GSTOP (GENSYM)))
		`(DO ((,VAR ,START (1+ ,VAR))
				(,GSTOP ,STOP))
			((> ,VAR ,GSTOP))
			,@BODY)))

;;;;;;;;;; STRUCTURE DEFINITION FOR A NODE ;;;;;;;;;;;;;;;;;;

(DEFSTRUCT NODE
    LEFT LLENGTH RIGHT RLENGTH ANCESTOR NODENUM)


;;;;;;;;;;;;;;;;; FUNCTIONS ;;;;;;;;;;;;

(DEFUN LEGAL-CHAR-P (CH)
    (IF (OR (ALPHANUMERICP CH)
	    (CHAR= CH #\$)
	    (CHAR= CH #\.)
	    (CHAR= CH #\*)
	    (CHAR= CH #\!)
	    (CHAR= CH #\@))
	T
	NIL))

(DEFUN REMOVE-CHAR (SYM C)
     (LET ((STRN (STRING SYM)))
 	(REMOVE-IF #'(LAMBDA (CH) (EQUAL CH C)) STRN :COUNT 1)))

(DEFUN MAKE-TREE-FROM-NEWICK (TREE)
    (LET ((HTABLE (MAKE-HASH-TABLE))
	  (ANC NIL)
	  (NNUM 0))
      (LABELS ((MTFN (TR AN) 
   ; (FORMAT T "Node number: ~D~%" NNUM)
    (SETF (GETHASH 'TOTAL-NODES HTABLE) NNUM)
    (LET ((N (MAKE-NODE 
		    :ANCESTOR    AN
		    :NODENUM     NNUM
		    :LLENGTH     (CADAR TR)
	            :LEFT        (IF (SYMBOLP (CAAAR TR))
		                     (REMOVE-CHAR (CAAAR TR) #\!)
		                     (INCF NNUM)))))
	
	(UNLESS (STRINGP (NODE-LEFT N))
	    (MTFN (CAAR TR) (NODE-NODENUM N)))
	
	(COND 
	    ((AND (= (LENGTH TR) 2)
		  (NUMBERP (CADAR TR))
		  (NUMBERP (CADADR TR)))
		(SETF (NODE-RLENGTH N)     (CADADR TR)
		      (NODE-RIGHT N)       (IF (SYMBOLP (CAAADR TR))
			                       (REMOVE-CHAR (CAAADR TR) #\!)
					       (INCF NNUM)))
		(UNLESS (STRINGP (NODE-RIGHT N))
		        (MTFN (CAADR TR) (NODE-NODENUM N))))
	    ((< 2 (LENGTH TR))
		(SETF (NODE-RLENGTH N)   0.0
		      (NODE-RIGHT N)     (INCF NNUM))
		(MTFN (CDR TR) (NODE-NODENUM N))))
	(SETF (GETHASH (NODE-NODENUM N) HTABLE) N))))
	(MTFN TREE NIL)) HTABLE))


    

(DEFUN MAKE-TREE-FROM-MATRIX (MATRIX LEAFLIST)
    (LET* ((NNUM 0)
	   (HTABLE (MAKE-HASH-TABLE)))
	(LABELS ((MTFM (MAT A)
		    (LET ((DC (DECOMP-MATRIX MAT))
			  (N  (MAKE-NODE)))
			(SETF (NODE-NODENUM N)  NNUM
			      (NODE-ANCESTOR N) A)
			;(FORMAT T "Node number: ~D~%" (NODE-NODENUM N))
			(SETF (GETHASH 'TOTAL-NODES HTABLE) (NODE-NODENUM N))
			(COND ((AND (= 1 (SECOND (ARRAY-DIMENSIONS (FIRST DC))))
				    (= 1 (SECOND (ARRAY-DIMENSIONS (SECOND DC)))))
				(SETF (NODE-LLENGTH N) (AREF (FIRST DC) 0 0)
				      (NODE-RLENGTH N) (AREF (SECOND DC) 0 0))
				(WHEN LEAFLIST (SETF (NODE-LEFT N) (STRING (CAR LEAFLIST))
				                     (NODE-RIGHT N) (STRING (CADR LEAFLIST))
				                     LEAFLIST (CDDR LEAFLIST)))
				(SETF (GETHASH (NODE-NODENUM N) HTABLE) N))

			    ; LOOK AT SECOND ELEMENT OF DIM SINCE = 0 IF EMPTY MATRIX
			    ((AND (= 1 (SECOND (ARRAY-DIMENSIONS (FIRST DC))))
				  (< 1 (SECOND (ARRAY-DIMENSIONS (SECOND DC)))))
				(SETF (NODE-LLENGTH N) (AREF (FIRST DC) 0 0)
				      (NODE-RLENGTH N) (MIN (SECOND DC))
				      (NODE-RIGHT N)   (INCF NNUM))
				(WHEN LEAFLIST (SETF (NODE-LEFT N)    (STRING (CAR LEAFLIST))
				                     LEAFLIST         (CDR LEAFLIST)))
				(MTFM (- (SECOND DC) (MIN (SECOND DC))) (NODE-NODENUM N))			 
				(SETF (GETHASH (NODE-NODENUM N) HTABLE) N))
				

			    ((AND (< 1 (SECOND (ARRAY-DIMENSIONS (FIRST DC))))
				  (= 1 (SECOND (ARRAY-DIMENSIONS (SECOND DC)))))
				(SETF (NODE-LLENGTH N) (MIN (FIRST DC))
				      (NODE-RLENGTH N) (AREF (SECOND DC) 0 0)
				      (NODE-LEFT N)    (INCF NNUM))

				 (MTFM (- (FIRST DC) (MIN (FIRST DC))) (NODE-NODENUM N))
				(WHEN LEAFLIST (SETF (NODE-RIGHT N)   (STRING (CAR LEAFLIST))
				                     LEAFLIST         (CDR LEAFLIST)))
				(SETF (GETHASH (NODE-NODENUM N) HTABLE) N))


			    ((AND (< 1 (SECOND (ARRAY-DIMENSIONS (FIRST DC))))
				  (< 1 (SECOND (ARRAY-DIMENSIONS (SECOND DC)))))
				(SETF (NODE-LLENGTH N) (MIN (FIRST DC))
				      (NODE-RLENGTH N) (MIN (SECOND DC))
				      (NODE-LEFT N)    (INCF NNUM))
				  (MTFM (- (FIRST DC) (MIN (FIRST DC))) (NODE-NODENUM N))
				  (SETF    (NODE-RIGHT N)   (INCF NNUM))
				  (MTFM (- (SECOND DC) (MIN (SECOND DC))) (NODE-NODENUM N))
				   (SETF    (GETHASH (NODE-NODENUM N) HTABLE) N))))))
	    (MTFM MATRIX NIL)
	    HTABLE)))


(DEFUN GET-ANCESTORS (N TABLE)
    (COND ((AND (STRINGP (NODE-LEFT N))
		(STRINGP (NODE-RIGHT N)))
	    (APPEND  (LIST (COMBINE (NODE-LEFT N) (LIST-ANCESTORS N TABLE)))
		(LIST (COMBINE (NODE-RIGHT N) (LIST-ANCESTORS N TABLE)))))

	((AND (STRINGP (NODE-LEFT N))
		(NUMBERP (NODE-RIGHT N)))
	    (APPEND (LIST (COMBINE (NODE-LEFT N) (LIST-ANCESTORS N TABLE)))
		(GET-ANCESTORS (GETHASH (NODE-RIGHT N) TABLE) TABLE)))

	((AND (NUMBERP (NODE-LEFT N))
		(STRINGP (NODE-RIGHT N)))
	    (APPEND  (GET-ANCESTORS (GETHASH (NODE-LEFT N) TABLE) TABLE)
		(LIST (COMBINE (NODE-RIGHT N) (LIST-ANCESTORS N TABLE)))))

	((AND (NUMBERP (NODE-LEFT N))
		(NUMBERP (NODE-RIGHT N)))
	    (APPEND (GET-ANCESTORS (GETHASH (NODE-LEFT N) TABLE) TABLE)
		(GET-ANCESTORS (GETHASH (NODE-RIGHT N) TABLE) TABLE)))))


(DEFUN LIST-ANCESTORS (N HTABLE)
    "CALLED BY GET-ANCESTORS"
    (COND 	((NULL (NODE-ANCESTOR N))
	    (LIST (NODE-NODENUM N)))

	((AND (STRINGP (NODE-LEFT N))
		(STRINGP (NODE-RIGHT N)))
	    (CONS (NODE-NODENUM N)
		(LIST-ANCESTORS (GETHASH (NODE-ANCESTOR N) HTABLE) HTABLE)))

	((STRINGP (NODE-LEFT N))
	    (CONS (NODE-NODENUM N)
		(LIST-ANCESTORS (GETHASH (NODE-ANCESTOR N) HTABLE) HTABLE)))

	((STRINGP (NODE-RIGHT N))
	    (CONS (NODE-NODENUM N)
		(LIST-ANCESTORS (GETHASH (NODE-ANCESTOR N) HTABLE) HTABLE)))

	(T  (CONS (NODE-NODENUM N)
		(LIST-ANCESTORS (GETHASH (NODE-ANCESTOR N) HTABLE) HTABLE)))))

(DEFUN DSC-MATRIX (TABLE)
    (LET* ((MAT (MRC-ANCESTORS (GET-ANCESTORS (GETHASH 0 TABLE) TABLE)))
	    (RES (MAKE-ARRAY (ARRAY-DIMENSIONS MAT))))
	(DOTIMES (I (FIRST (ARRAY-DIMENSIONS MAT)))
	    (DOTIMES (J (FIRST (ARRAY-DIMENSIONS MAT)))
		(SETF (AREF RES I J) (FIND-NODE-LENGTH (AREF MAT I J)
(GETHASH 0 TABLE) TABLE)
		    (AREF RES J I) (FIND-NODE-LENGTH (AREF MAT J I)
(GETHASH 0 TABLE) TABLE)))) RES))

(DEFUN LIST-TIPS (TBLE)
    (LABELS ((LT (TREE)
		(COND ((AND (STRINGP (NODE-LEFT TREE))
			    (STRINGP (NODE-RIGHT TREE)))
			(LIST (NODE-LEFT TREE)
			    (NODE-RIGHT TREE)))

		    ((STRINGP (NODE-LEFT TREE))
			(CONS (NODE-LEFT TREE)
			    (LT (GETHASH (NODE-RIGHT TREE) TBLE))))

		    ((STRINGP (NODE-RIGHT TREE))
			(APPEND (LT (GETHASH (NODE-LEFT TREE) TBLE)) (LIST (NODE-RIGHT TREE))))

		    (T (APPEND (LT (GETHASH (NODE-LEFT TREE) TBLE))
			    (LT (GETHASH (NODE-RIGHT TREE) TBLE)))))))
	(LT (GETHASH 0 TBLE))))


(DEFUN PRINT-NEWICK (TBL FORMSTRING)
    (LABELS ((P-NEWICK (NODENUM)
		;(FORMAT T "Node Number = ~D~%" NODENUM)
		(LET ((TR (GETHASH NODENUM TBL)))
		(COND
		    ((AND (STRINGP (NODE-LEFT TR))
			    (STRINGP (NODE-RIGHT TR)))
			(FORMAT NIL FORMSTRING
			    (NODE-LEFT TR)
			    (NODE-LLENGTH TR)
			    (NODE-RIGHT TR)
			    (NODE-RLENGTH TR)))

		    ((AND (NUMBERP (NODE-LEFT TR))
			    (STRINGP (NODE-RIGHT TR)))
			(FORMAT NIL FORMSTRING
			    (P-NEWICK (NODE-LEFT TR))
			    (NODE-LLENGTH TR)
			    (NODE-RIGHT TR)
			    (NODE-RLENGTH TR)))

		    ((AND (STRINGP (NODE-LEFT TR))
			    (NUMBERP (NODE-RIGHT TR)))
			(FORMAT NIL FORMSTRING (NODE-LEFT TR)
			    (NODE-LLENGTH TR)
			    (P-NEWICK (NODE-RIGHT TR))
			    (NODE-RLENGTH TR)))

		    ((AND (NUMBERP (NODE-LEFT TR))
			    (NUMBERP (NODE-RIGHT TR)))
			(FORMAT NIL FORMSTRING
			    (P-NEWICK (NODE-LEFT TR))
			    (NODE-LLENGTH TR)
			    (P-NEWICK (NODE-RIGHT TR))
			    (NODE-RLENGTH TR)))))))

	(CONCATENATE 'STRING (P-NEWICK 0) ";")))


(DEFUN READ-TREE ()
    (WITH-OPEN-FILE (F (PROGN (MESSAGE-DIALOG "Choose a Tree File!") (OPEN-FILE-DIALOG)) :DIRECTION :INPUT)
    (LET* ((RESVEC (MAKE-ARRAY 50000))
		(NOWHITE (DO* ((CHAR (READ-CHAR F) (READ-CHAR F))
			    (NEXTCHAR (PEEK-CHAR :STREAM F) (UNLESS (CHAR= NEXTCHAR #\;)
				    (PEEK-CHAR :STREAM F)))
			    (RES (LIST CHAR) (COND ((CHAR= CHAR #\SPACE) RES)
				    ((CHAR= CHAR #\TAB) RES)
				    ((CHAR= CHAR #\NEWLINE) RES)
				    (T (CONS CHAR RES)))))
			((CHAR= CHAR #\;) (NREVERSE RES)))))
	    (DO* ((N 0 (1+ N))
		    (C 0 (1+ C))
		    (CHAR (NTH N NOWHITE) (NTH N NOWHITE))
		    (NEXTCHAR (NTH (1+ N) NOWHITE) (NTH (1+ N) NOWHITE))
		    (PREVCHAR (NTH 0 NOWHITE) (NTH (1- N) NOWHITE))
		    (RES (SETF (SVREF RESVEC C) CHAR) ;FIRST BRACKET

			(COND ((CHAR= CHAR #\()
				(SETF (SVREF RESVEC C)       #\(
				      (SVREF RESVEC (1+ C))  #\(
				      C                      (1+ C)))

			    ((AND (LEGAL-CHAR-P CHAR) (CHAR= PREVCHAR #\())
;BEGINNING OF A NAME
				(SETF (SVREF RESVEC C)       #\(
				      (SVREF RESVEC (1+ C))  #\(
				      (SVREF RESVEC (+ 2 C)) #\!
				      (SVREF RESVEC (+ 3 C)) CHAR
				      C                      (+ 3 C)))

			    ((CHAR= CHAR #\:)
				
				(SETF (SVREF RESVEC C)       #\)))
				      

			    ((AND (CHAR= CHAR #\,) (LEGAL-CHAR-P NEXTCHAR))
				(SETF (SVREF RESVEC C)       #\)
				      (SVREF RESVEC (1+ C))  #\(
				      (SVREF RESVEC (+ 2 C)) #\(
				      (SVREF RESVEC (+ 3 C)) #\!
				      C                      (+ 3 C)))

			    ((CHAR= CHAR #\,)
				(SETF (SVREF RESVEC C)       #\)))

			    ((CHAR= CHAR #\;)
				(SETF (SVREF RESVEC C)       #\)))

			    (T  (SETF (SVREF RESVEC C)      CHAR)))))
		((CHAR= CHAR #\;) (READ-FROM-STRING (COERCE (SELECT RESVEC
(ISEQ (1+ C))) 'STRING)))))))

(DEFUN MRC-ANCESTORS (LST)
    "CALCULATE LIST OF MOST RECENT COMMON ANCESTORS"
    (LET* ((L (MAPCAR #'FIRST LST))
	    (MAT (MAKE-ARRAY (LIST (LENGTH L) (LENGTH L)))))
	(DOTIMES (I (LENGTH L))
	    (DOTIMES (J (LENGTH L))
		(IF (= I J)
		    (SETF (AREF MAT I J) (CAR (NTH I LST)))
		    (SETF (AREF MAT I J)
			(MAX (INTERSECTION (CDR (NTH I LST))
				(CDR (NTH J LST))))))))
	MAT))

(DEFUN FIND-NODE-LENGTH (NODE TREE HTABLE)
    "FIND THE DISTANCE OF A NODE TO THE ROOT"
    (COND ((EQUAL NODE (NODE-LEFT TREE))
	    (+ (NODE-ROOT-LENGTH (NODE-NODENUM TREE) HTABLE) (NODE-LLENGTH TREE)))

	((EQUAL NODE (NODE-RIGHT TREE))
	    (+ (NODE-ROOT-LENGTH (NODE-NODENUM TREE) HTABLE) (NODE-RLENGTH TREE)))

	((EQUAL NODE (NODE-NODENUM TREE))
	    (NODE-ROOT-LENGTH (NODE-NODENUM TREE) HTABLE))

	((AND (STRINGP (NODE-LEFT TREE))
		(NUMBERP (NODE-RIGHT TREE)))
	    (FIND-NODE-LENGTH NODE (GETHASH (NODE-RIGHT TREE) HTABLE) HTABLE))

	((AND (STRINGP (NODE-RIGHT TREE))
		(NUMBERP (NODE-LEFT TREE)))
	    (FIND-NODE-LENGTH NODE (GETHASH (NODE-LEFT TREE) HTABLE) HTABLE))

	((AND (NUMBERP (NODE-LEFT TREE))
		(NUMBERP (NODE-RIGHT TREE)))
	    (LET ((X (FIND-NODE-LENGTH NODE (GETHASH (NODE-RIGHT TREE) HTABLE) HTABLE)))
		(IF X X (FIND-NODE-LENGTH NODE (GETHASH (NODE-LEFT TREE) HTABLE) HTABLE))))))

(DEFUN NODE-ROOT-LENGTH (NNUM HTABLE &OPTIONAL (BRLENGTH 0))
    "CALLED BY FIND-NODE-LENGTH"
    (COND ((NULL (NODE-ANCESTOR (GETHASH NNUM HTABLE))) BRLENGTH)

	((EQUAL (NODE-LEFT (GETHASH (NODE-ANCESTOR (GETHASH NNUM HTABLE)) HTABLE)) NNUM)
	    (INCF BRLENGTH (NODE-LLENGTH (GETHASH (NODE-ANCESTOR (GETHASH NNUM HTABLE)) HTABLE)))
		(NODE-ROOT-LENGTH (NODE-ANCESTOR (GETHASH NNUM HTABLE)) HTABLE BRLENGTH))

	((EQUAL (NODE-RIGHT (GETHASH (NODE-ANCESTOR (GETHASH NNUM HTABLE)) HTABLE)) NNUM)
	    (INCF BRLENGTH (NODE-RLENGTH (GETHASH (NODE-ANCESTOR (GETHASH NNUM HTABLE)) HTABLE)))
		(NODE-ROOT-LENGTH (NODE-ANCESTOR (GETHASH NNUM HTABLE)) HTABLE BRLENGTH))))

(DEFUN DECOMP-MATRIX (MAT)
    (DO ((MAX (1- (FIRST (ARRAY-DIMENSIONS MAT))))
	 (TOLERANCE *TOLERANCE*)
	 (I 0 (1+ I)))
	 ((<= (+ (SUM (SELECT MAT (ISEQ (1+ I) MAX) I)
		     (SUM (SELECT MAT I (ISEQ (1+ I) MAX)))))
	      TOLERANCE)
  (LIST (SELECT MAT (ISEQ 0 I) (ISEQ 0 I))
        (SELECT MAT (ISEQ (1+ I)  MAX) (ISEQ (1+ I) MAX))))))


(DEFUN REPSYM (X)
	(DO ((I 0 (1+ I))
		(ANS NIL (CONS (GENSYM) ANS)))
	((= I X) ANS)))


(DEFUN OU (V D)
    (IF (< (ABS (- D 1)) .00001)
	(/ V (^ (DETERMINANT V) (/ 1 (FIRST (ARRAY-DIMENSIONS V)))))
    (LET* ((INITV (/ V (MAX V)))
	    (TIPSUM (+ (OUTER-PRODUCT (DIAGONAL INITV) (REPEAT 1
(ARRAY-DIMENSION INITV 0)))
		    (OUTER-PRODUCT (REPEAT 1 (ARRAY-DIMENSION INITV 0)) (DIAGONAL INITV))))
	    (VDIAG (/ (- 1 (^ D (* 2 (DIAGONAL (DIAGONAL INITV))))) (- 1 (^ D 2))))
	    (VOFFDIAG (/ (* (^ D TIPSUM) (- 1 (^ D (* 2 INITV)))) (- 1 (^ D 2))))
	    (VOFFDIAG2 (- VOFFDIAG (DIAGONAL (DIAGONAL VOFFDIAG))))
	    (VIN (+ VDIAG VOFFDIAG2)))
	(/ VIN (^ (DETERMINANT VIN) (/ 1 (FIRST (ARRAY-DIMENSIONS V))))))))

(DEFUN BLENGTHS (TABLE)
    (LET ((LST NIL))
      (MAPHASH #'(LAMBDA (K V) (PUSH (+ (NODE-RLENGTH V) (NODE-LLENGTH V)) LST)) TABLE)
      (SUM LST)))

(DEFUN TIPLENGTHS (TABLE)
	(LET ((LST NIL))
		(MAPHASH #'(LAMBDA (K V) (PUSH (+ (IF (STRINGP (NODE-LEFT V)) (NODE-LLENGTH V) 0)
							              (IF (STRINGP (NODE-RIGHT V)) (NODE-RLENGTH V) 0))
												LST)) TABLE)
								(SUM LST)))
    
(DEFUN MENU ()
    (CHOOSE-ITEM-DIALOG "Choose an action:" '("Tree to Matrix" "Matrix to Tree")))
(DEFUN MENU2 ()
    (CHOOSE-ITEM-DIALOG "Scientific Notation?" '("Yes" "No")))

(DEFUN MAIN ()                                                                                                                
     (DO ((M (MENU) (MENU)))
         ((NULL M) (MESSAGE-DIALOG "Goodbye!"))                                                                            
         (COND ((= M 0)
                 (WITH-OPEN-FILE (STR (GET-STRING-DIALOG "Name of output file for matrix:" :INITIAL "MATRIX.OUT") :DIRECTION :OUTPUT)
		 (WITH-OPEN-FILE (STR2 (GET-STRING-DIALOG "Name of output file for tip names:" :INITIAL "TIPS.OUT") :DIRECTION :OUTPUT) 
		      (LET* ((M2 (MENU2))
			     (TBLE (MAKE-TREE-FROM-NEWICK (READ-TREE)))
			     (MAT (DSC-MATRIX TBLE))
			     (MAXMAT (1- (FIRST (ARRAY-DIMENSIONS MAT))))
			     (TIPS (LIST-TIPS TBLE)))
			(FOR I 0 MAXMAT
			    (FOR J 0 MAXMAT
				(IF (= M2 0) (FORMAT STR "~E " (AREF MAT I J))
				             (FORMAT STR "~F " (AREF MAT I J))))
			    (FORMAT STR "~%"))
		    (DOLIST (I TIPS)
			(FORMAT STR2 "~S~%" I))
		     (MESSAGE-DIALOG "Done!")))))
	     ((= M 1)                                                                               
                 (WITH-OPEN-FILE (STR (GET-STRING-DIALOG "Name of output file for tree:" :INITIAL "TREE.BRK") :DIRECTION :OUTPUT)
		     (LET* ((DAT (PROGN (MESSAGE-DIALOG "Choose a Matrix File!") (READ-DATA-FILE)))
			    (N (PROGN (MESSAGE-DIALOG "Choose a Tip Names File!") (READ-DATA-COLUMNS)))
			    (NAMES (FIRST N))
			    (M2 (MENU2))
			    (DIM (FLOOR (SQRT (LENGTH DAT))))
			    (MAT (MAKE-ARRAY (LIST DIM DIM) :INITIAL-CONTENTS DAT)))
			(FORMAT STR (PRINT-NEWICK (MAKE-TREE-FROM-MATRIX MAT NAMES)
				                  (IF (= M2 0) "(~A:~E, ~A:~E)" "(~A:~D, ~A:~D)")))
			(MESSAGE-DIALOG "Done!")))))))
			

(MAIN)


